package raft.node

import com.google.common.eventbus.DeadEvent
import com.google.common.eventbus.Subscribe
import com.google.common.util.concurrent.FutureCallback
import org.slf4j.LoggerFactory
import raft.log.InstallSnapshotState
import raft.log.entry.Entry
import raft.log.entry.EntryMeta
import raft.log.entry.GroupConfigEntry
import raft.log.entry.RemoveNodeEntry
import raft.log.event.GroupConfigEntryBatchRemovedEvent
import raft.log.event.GroupConfigEntryCommittedEvent
import raft.log.event.GroupConfigEntryFromLeaderAppendEvent
import raft.log.event.SnapshotGenerateEvent
import raft.log.snapshot.EntryInSnapshotException
import raft.log.statemachine.StateMachine
import raft.node.role.*
import raft.node.store.NodeStore
import raft.node.task.*
import raft.rpc.message.*
import raft.schedule.ElectionTimeout
import raft.schedule.LogReplicationTask
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe

/**
 * Node implementation.
 *
 * @see NodeContext
 */
@ThreadSafe
class NodeImpl
/**
 * Create with context.
 *
 * @param context context
 */ constructor(
    /**
     * Get context.
     *
     * @return context
     */
    val context: NodeContext
) : Node {

    @GuardedBy("this")
    private var started = false

    @Volatile
    private var role: AbstractNodeRole? = null
    private val roleListeners: MutableList<NodeRoleListener> = CopyOnWriteArrayList()

    // NewNodeCatchUpTask and GroupConfigChangeTask related
    private val newNodeCatchUpTaskContext: NewNodeCatchUpTaskContext = NewNodeCatchUpTaskContextImpl()
    private val newNodeCatchUpTaskGroup = NewNodeCatchUpTaskGroup()
    private val groupConfigChangeTaskContext: GroupConfigChangeTaskContext = GroupConfigChangeTaskContextImpl()

    @Volatile
    private var groupConfigChangeTaskHolder = GroupConfigChangeTaskHolder()

    @Synchronized
    override fun registerStateMachine(stateMachine: StateMachine?) {
        context.log!!.setStateMachine(stateMachine)
    }

    override val roleNameAndLeaderId: RoleNameAndLeaderId?
        get() = role!!.getNameAndLeaderId(context.selfId)

    /**
     * Get role state.
     *
     * @return role state
     */
    val roleState: RoleState
        get() = role?.state!!

    override fun addNodeRoleListener(listener: NodeRoleListener?) {
        if (listener != null) {
            roleListeners.add(listener)
        }
    }

    @Synchronized
    override fun start() {
        if (started) {
            return
        }
        context.eventBus!!.register(this)
        context.connector!!.initialize()

        // load term, votedFor from store and become follower
        val store: NodeStore = context.store()!!
        changeToRole(FollowerNodeRole(store.getTerm(), store.getVotedFor(), null, scheduleElectionTimeout()))
        started = true
    }

    override fun appendLog(commandBytes: ByteArray?) {
        ensureLeader()
        context.taskExecutor()?.submit({
            role?.term?.let { context.log()?.appendEntry(it, commandBytes) }
            doReplicateLog()
        }, LOGGING_FUTURE_CALLBACK)
    }

    override fun addNode(endpoint: NodeEndpoint?): GroupConfigChangeTaskReference? {
        ensureLeader()

        // self cannot be added
        if (endpoint != null) {
            require(!context.selfId()?.equals(endpoint.id)!!) { "new node cannot be self" }
        }
        val newNodeCatchUpTask = endpoint?.let { context.config?.let { it1 ->
            NewNodeCatchUpTask(
                newNodeCatchUpTaskContext, it,
                it1
            )
        } }

        // task for node exists
        if (endpoint != null) {
            newNodeCatchUpTask?.let { newNodeCatchUpTaskGroup.add(it) }?.let {
                require(it) {
                    "node " + endpoint.id.toString() + " is adding"
                }
            }
        }

        // catch up new server
        // this will be run in caller thread
        val newNodeCatchUpTaskResult: NewNodeCatchUpTaskResult
        try {
            newNodeCatchUpTaskResult = newNodeCatchUpTask!!.call()
            when (newNodeCatchUpTaskResult.state) {
                NewNodeCatchUpTaskResult.State.REPLICATION_FAILED -> return FixedResultGroupConfigTaskReference(
                    GroupConfigChangeTaskResult.REPLICATION_FAILED
                )
                NewNodeCatchUpTaskResult.State.TIMEOUT -> return FixedResultGroupConfigTaskReference(
                    GroupConfigChangeTaskResult.TIMEOUT
                )
            }
        } catch (e: Exception) {
            if (e !is InterruptedException) {
                logger.warn("failed to catch up new node " + endpoint!!.id, e)
            }
            return FixedResultGroupConfigTaskReference(GroupConfigChangeTaskResult.ERROR)
        }

        // new server caught up
        // wait for previous group config change
        // it will wait forever by default, but you can change to fixed timeout by setting in NodeConfig
        val result = awaitPreviousGroupConfigChangeTask()
        if (result != null) {
            return FixedResultGroupConfigTaskReference(result)
        }

        // submit group config change task
        synchronized(this) {


            // it will happen when try to add two or more nodes at the same time
            check(groupConfigChangeTaskHolder.isEmpty) { "group config change concurrently" }
            val addNodeTask = AddNodeTask(groupConfigChangeTaskContext, endpoint, newNodeCatchUpTaskResult)
            val future: Future<GroupConfigChangeTaskResult> =
                context.groupConfigChangeTaskExecutor()!!.submit(addNodeTask) as Future<GroupConfigChangeTaskResult>
            val reference: GroupConfigChangeTaskReference = FutureGroupConfigChangeTaskReference(future)
            groupConfigChangeTaskHolder = GroupConfigChangeTaskHolder(addNodeTask, reference)
            return reference
        }
    }

    /**
     * Await previous group config change task.
     *
     * @return `null` if previous task done, otherwise error or timeout
     * @see GroupConfigChangeTaskResult.ERROR
     *
     * @see GroupConfigChangeTaskResult.TIMEOUT
     */
    private fun awaitPreviousGroupConfigChangeTask(): GroupConfigChangeTaskResult? {
        return try {
            groupConfigChangeTaskHolder.awaitDone(context.config()!!.previousGroupConfigChangeTimeout.toLong())
            null
        } catch (ignored: InterruptedException) {
            GroupConfigChangeTaskResult.ERROR
        } catch (ignored: TimeoutException) {
            logger.info("previous cannot complete within timeout")
            GroupConfigChangeTaskResult.TIMEOUT
        }
    }

    /**
     * Ensure leader status
     *
     * @throws NotLeaderException if not leader
     */
    private fun ensureLeader() {
        val result = role!!.getNameAndLeaderId(context.selfId())
        if (result!!.roleName === RoleName.LEADER) {
            return
        }
        val endpoint: NodeEndpoint? =
            if (result!!.leaderId != null) context.group()!!.findMember(result.leaderId!!).endpoint else null
        throw NotLeaderException(result.roleName, endpoint)
    }

    override fun removeNode(id: NodeId?): GroupConfigChangeTaskReference? {
        ensureLeader()

        // await previous group config change task
        val result = awaitPreviousGroupConfigChangeTask()
        if (result != null) {
            return FixedResultGroupConfigTaskReference(result)
        }

        // submit group config change task
        synchronized(this) {


            // it will happen when try to remove two or more nodes at the same time
            check(groupConfigChangeTaskHolder.isEmpty) { "group config change concurrently" }
            val task = RemoveNodeTask(groupConfigChangeTaskContext, id!!)
            val future: Future<GroupConfigChangeTaskResult> =
                context.groupConfigChangeTaskExecutor()!!.submit(task) as Future<GroupConfigChangeTaskResult>
            val reference: GroupConfigChangeTaskReference = FutureGroupConfigChangeTaskReference(future)
            groupConfigChangeTaskHolder = GroupConfigChangeTaskHolder(task, reference)
            return reference
        }
    }

    /**
     * Cancel current group config change task
     */
    @Synchronized
    fun cancelGroupConfigChangeTask() {
        if (groupConfigChangeTaskHolder.isEmpty) {
            return
        }
        logger.info("cancel group config change task")
        groupConfigChangeTaskHolder.cancel()
        groupConfigChangeTaskHolder = GroupConfigChangeTaskHolder()
    }

    /**
     * Election timeout
     *
     *
     * Source: scheduler
     *
     */
    fun electionTimeout() {
        context.taskExecutor()!!.submit({ doProcessElectionTimeout() }, LOGGING_FUTURE_CALLBACK)
    }

    private fun doProcessElectionTimeout() {
        if (role!!.getName() === RoleName.LEADER) {
            logger.warn("node {}, current role is leader, ignore election timeout", context.selfId())
            return
        }

        // follower: start election
        // candidate: restart election
        val newTerm: Int = role!!.term + 1
        role!!.cancelTimeoutOrTask()
        if (context.group()!!.isStandalone()) {
            if (context.mode() === NodeMode.STANDBY) {
                logger.info("starts with standby mode, skip election")
            } else {

                // become leader
                logger.info("become leader, term {}", newTerm)
                resetReplicatingStates()
                changeToRole(LeaderNodeRole(newTerm, scheduleLogReplicationTask()))
                context.log()!!.appendEntry(newTerm) // no-op log
            }
        } else {
            logger.info("start election")
            changeToRole(CandidateNodeRole(newTerm, scheduleElectionTimeout()))

            // request vote
            val lastEntryMeta: EntryMeta = context.log()!!.lastEntryMeta!!
            val rpc = RequestVoteRpc()
            rpc.term = newTerm
            rpc.candidateId = context.selfId()!!
            rpc.lastLogIndex = lastEntryMeta.index
            rpc.lastLogTerm = lastEntryMeta.term
            context.connector()!!.sendRequestVote(rpc, context.group()!!.listEndpointOfMajorExceptSelf())
        }
    }

    /**
     * Become follower.
     *
     * @param term                    term
     * @param votedFor                voted for
     * @param leaderId                leader id
     * @param scheduleElectionTimeout schedule election timeout or not
     */
    private fun becomeFollower(term: Int, votedFor: NodeId?, leaderId: NodeId?, scheduleElectionTimeout: Boolean) {
        role!!.cancelTimeoutOrTask()
        if (leaderId != null && !leaderId.equals(role!!.getLeaderId(context.selfId()))) {
            logger.info("current leader is {}, term {}", leaderId, term)
        }
        val electionTimeout = if (scheduleElectionTimeout) scheduleElectionTimeout() else ElectionTimeout.NONE
        changeToRole(FollowerNodeRole(term, votedFor!!, leaderId, electionTimeout))
    }

    /**
     * Change role.
     *
     * @param newRole new role
     */
    private fun changeToRole(newRole: AbstractNodeRole) {
        if (!isStableBetween(role, newRole)) {
            logger.debug("node {}, role state changed -> {}", context.selfId(), newRole)
            val state: RoleState = newRole.state!!

            // update store
            val store: NodeStore = context.store()!!
            store.setTerm(state.term)
            store.setVotedFor(state.votedFor!!)

            // notify listeners
            roleListeners.forEach(Consumer { l: NodeRoleListener ->
                l.nodeRoleChanged(
                    state
                )
            })
        }
        role = newRole
    }

    /**
     * Check if stable between two roles.
     *
     *
     * It is stable when role name not changed and role state except timeout/task not change.
     *
     *
     *
     * If role state except timeout/task not changed, it should not update store or notify listeners.
     *
     *
     * @param before role before
     * @param after  role after
     * @return true if stable, otherwise false
     * @see AbstractNodeRole.stateEquals
     */
    private fun isStableBetween(before: AbstractNodeRole?, after: AbstractNodeRole?): Boolean {
        assert(after != null)
        return before != null && before.stateEquals(after!!)
    }

    /**
     * Schedule election timeout.
     *
     * @return election timeout
     */
    private fun scheduleElectionTimeout(): ElectionTimeout {
        return context.scheduler()!!.scheduleElectionTimeout { electionTimeout() }
    }

    /**
     * Reset replicating states.
     */
    private fun resetReplicatingStates() {
        context.group()!!.resetReplicatingStates(context.log()!!.nextIndex)
    }

    /**
     * Schedule log replication task.
     *
     * @return log replication task
     */
    private fun scheduleLogReplicationTask(): LogReplicationTask {
        return context.scheduler()!!.scheduleLogReplicationTask { replicateLog() }
    }

    /**
     * Replicate log.
     *
     *
     * Source: scheduler.
     *
     */
    fun replicateLog() {
        context.taskExecutor()!!.submit(this::doReplicateLog, LOGGING_FUTURE_CALLBACK)
    }


    private fun doReplicateLog() {
        // just advance commit index if is unique node
        logger.debug("replicate log")
        for (member in context.group!!.listReplicationTarget()!!) {
            doReplicateLog(member as GroupMember)
        }
    }

    private fun doReplicateLog(member: GroupMember) {
        val rpc: AppendEntriesRpc = AppendEntriesRpc()
        rpc.term = role!!.term
        rpc.leaderId = context.selfId!!
        rpc.prevLogIndex = 0
        rpc.prevLogTerm = 0
        rpc.leaderCommit = 0
        context.connector!!.sendAppendEntries(rpc, member.endpoint)
    }

    /**
     * Replicate log to specified node.
     *
     *
     * Normally it will send append entries rpc to node. And change to install snapshot rpc if entry in snapshot.
     *
     *
     * @param member     node
     * @param maxEntries max entries
     * @see EntryInSnapshotException
     */
    private fun doReplicateLog(member: GroupMember, maxEntries: Int) {
        member.replicateNow()
        try {
            val rpc = context.log()!!
                .createAppendEntriesRpc(role!!.term, context.selfId(), member.nextIndex, maxEntries)
            context.connector()!!.sendAppendEntries(rpc!!, member.endpoint)
        } catch (ignored: EntryInSnapshotException) {
            logger.debug(
                "log entry {} in snapshot, replicate with install snapshot RPC",
                member.nextIndex
            )
            val rpc = context.log()!!
                .createInstallSnapshotRpc(role!!.term, context.selfId(), 0, context.config()!!.snapshotDataLength)
            context.connector()!!.sendInstallSnapshot(rpc!!, member.endpoint)
        }
    }

    /**
     * Receive request vote rpc.
     *
     *
     * Source: connector.
     *
     *
     * @param rpcMessage rpc message
     */
    @Subscribe
    fun onReceiveRequestVoteRpc(rpcMessage: RequestVoteRpcMessage) {
        context.taskExecutor()!!.submit(
            { context.connector()!!.replyRequestVote(doProcessRequestVoteRpc(rpcMessage), rpcMessage) },
            LOGGING_FUTURE_CALLBACK
        )
    }

    private fun doProcessRequestVoteRpc(rpcMessage: RequestVoteRpcMessage): RequestVoteResult {

        // skip non-major node, it maybe removed node
        if (!rpcMessage.sourceNodeId?.let { context.group()!!.isMemberOfMajor(it) }!!) {
            logger.warn(
                "receive request vote rpc from node {} which is not major node, ignore",
                rpcMessage.sourceNodeId
            )
            return RequestVoteResult(role!!.term, false)
        }

        // reply current term if result's term is smaller than current one
        val rpc = rpcMessage.get()
        if (rpc!!.term < role!!.term) {
            logger.debug("term from rpc < current term, don't vote ({} < {})", rpc.term, role!!.term)
            return RequestVoteResult(role!!.term, false)
        }

        // step down if result's term is larger than current term
        if (rpc.term > role!!.term) {
            val voteForCandidate: Boolean = !context.log()!!.isNewerThan(rpc.lastLogIndex, rpc.lastLogTerm)
            becomeFollower(rpc.term, if (voteForCandidate) rpc.candidateId else null, null, true)
            return RequestVoteResult(rpc.term, voteForCandidate)
        }
        assert(rpc.term == role!!.term)
        return when (role!!.getName()) {
            RoleName.FOLLOWER -> {
                val follower = role as FollowerNodeRole?
                val votedFor: NodeId = follower!!.votedFor
                // reply vote granted for
                // 1. not voted and candidate's log is newer than self
                // 2. voted for candidate
                if (votedFor == rpc.candidateId
                ) {
                    becomeFollower(role!!.term, rpc.candidateId, null, true)
                    return RequestVoteResult(rpc.term, true)
                }
                RequestVoteResult(role!!.term, false)
            }
            RoleName.CANDIDATE, RoleName.LEADER -> RequestVoteResult(role!!.term, false)
            else -> throw IllegalStateException("unexpected node role [" + role!!.getName().toString() + "]")
        }
    }

    /**
     * Receive request vote result.
     *
     *
     * Source: connector.
     *
     *
     * @param result result
     */
    @Subscribe
    fun onReceiveRequestVoteResult(result: RequestVoteResult) {
        context.taskExecutor()!!.submit({ doProcessRequestVoteResult(result) }, LOGGING_FUTURE_CALLBACK)
    }

    fun processRequestVoteResult(result: RequestVoteResult): Future<*> {
        return context.taskExecutor()!!.submit { doProcessRequestVoteResult(result) }
    }

    private fun doProcessRequestVoteResult(result: RequestVoteResult) {

        // step down if result's term is larger than current term
        if (result.getTerm() > role!!.term) {
            becomeFollower(result.getTerm(), null, null, true)
            return
        }

        // check role
        if (role!!.getName() !== RoleName.CANDIDATE) {
            logger.debug("receive request vote result and current role is not candidate, ignore")
            return
        }

        // do nothing if not vote granted
        if (!result.isVoteGranted()) {
            return
        }
        val currentVotesCount: Int = (role as CandidateNodeRole?)!!.votesCount + 1
        val countOfMajor: Int = context.group()!!.countOfMajor
        logger.debug("votes count {}, major node count {}", currentVotesCount, countOfMajor)
        role!!.cancelTimeoutOrTask()
        if (currentVotesCount > countOfMajor / 2) {

            // become leader
            logger.info("become leader, term {}", role!!.term)
            resetReplicatingStates()
            changeToRole(LeaderNodeRole(role!!.term, scheduleLogReplicationTask()))
            context.log()!!.appendEntry(role!!.term) // no-op log
            context.connector()!!.resetChannels() // close all inbound channels
        } else {

            // update votes count
            changeToRole(CandidateNodeRole(role!!.term, currentVotesCount, scheduleElectionTimeout()))
        }
    }

    /**
     * Receive append entries rpc.
     *
     *
     * Source: connector.
     *
     *
     * @param rpcMessage rpc message
     */
    @Subscribe
    fun onReceiveAppendEntriesRpc(rpcMessage: AppendEntriesRpcMessage) {
        context.taskExecutor()!!.submit(
            { context.connector()!!.replyAppendEntries(doProcessAppendEntriesRpc(rpcMessage), rpcMessage) },
            LOGGING_FUTURE_CALLBACK
        )
    }

    private fun doProcessAppendEntriesRpc(rpcMessage: AppendEntriesRpcMessage): AppendEntriesResult {
        val rpc = rpcMessage.get()

        // reply current term if term in rpc is smaller than current term
        if (rpc!!.term < role!!.term) {
            return AppendEntriesResult(rpc.messageId!!, role!!.term, false)
        }

        // if term in rpc is larger than current term, step down and append entries
        if (rpc.term > role!!.term) {
            becomeFollower(rpc.term, null, rpc.leaderId, true)
            return AppendEntriesResult(rpc.messageId!!, rpc.term, appendEntries(rpc))
        }
        assert(rpc.term == role!!.term)
        return when (role!!.getName()) {
            RoleName.FOLLOWER -> {

                // reset election timeout and append entries
                becomeFollower(rpc.term, (role as FollowerNodeRole?)!!.votedFor, rpc.leaderId, true)
                AppendEntriesResult(rpc.messageId!!, rpc.term, appendEntries(rpc))
            }
            RoleName.CANDIDATE -> {

                // more than one candidate but another node won the election
                becomeFollower(rpc.term, null, rpc.leaderId, true)
                AppendEntriesResult(rpc.messageId!!, rpc.term, appendEntries(rpc))
            }
            RoleName.LEADER -> {
                logger.warn("receive append entries rpc from another leader {}, ignore", rpc.leaderId)
                AppendEntriesResult(rpc.messageId!!, rpc.term, false)
            }
            else -> throw IllegalStateException("unexpected node role [" + role!!.getName().toString() + "]")
        }
    }

    /**
     * Append entries and advance commit index if possible.
     *
     * @param rpc rpc
     * @return `true` if log appended, `false` if previous log check failed, etc
     */
    private fun appendEntries(rpc: AppendEntriesRpc?): Boolean {
        val result: Boolean =
            context.log()!!.appendEntriesFromLeader(rpc!!.prevLogIndex, rpc.prevLogTerm, rpc.entries)
        if (result) {
            context.log()!!.advanceCommitIndex(Math.min(rpc.leaderCommit, rpc.lastEntryIndex), rpc.term)
        }
        return result
    }

    /**
     * Receive append entries result.
     *
     * @param resultMessage result message
     */
    @Subscribe
    fun onReceiveAppendEntriesResult(resultMessage: AppendEntriesResultMessage) {
        context.taskExecutor()!!.submit({ doProcessAppendEntriesResult(resultMessage) }, LOGGING_FUTURE_CALLBACK)
    }

    fun processAppendEntriesResult(resultMessage: AppendEntriesResultMessage): Future<*> {
        return context.taskExecutor()!!.submit { doProcessAppendEntriesResult(resultMessage) }
    }

    private fun doProcessAppendEntriesResult(resultMessage: AppendEntriesResultMessage) {
        val result = resultMessage.get()

        // step down if result's term is larger than current term
        if (result.term > role!!.term) {
            becomeFollower(result.term, null, null, true)
            return
        }

        // check role
        if (role!!.getName() !== RoleName.LEADER) {
            logger.warn(
                "receive append entries result from node {} but current node is not leader, ignore",
                resultMessage.getSourceNodeId()
            )
            return
        }

        // dispatch to new node catch up task by node id
        if (newNodeCatchUpTaskGroup.onReceiveAppendEntriesResult(resultMessage, context.log()!!.nextIndex)) {
            return
        }
        val sourceNodeId = resultMessage.getSourceNodeId()
        val member: GroupMember = context.group()!!.getMember(sourceNodeId)!!
        val rpc: AppendEntriesRpc = resultMessage.rpc
        if (result.isSuccess) {
            if (!member.isMajor) {  // removing node
                if (member.isRemoving) {
                    logger.debug("node {} is removing, skip", sourceNodeId)
                } else {
                    logger.warn(
                        "unexpected append entries result from node {}, not major and not removing",
                        sourceNodeId
                    )
                }
                member.stopReplicating()
                return
            }

            // peer
            // advance commit index if major of match index changed
            if (member.advanceReplicatingState(rpc.lastEntryIndex)) {
                context.log()!!.advanceCommitIndex(context.group()!!.matchIndexOfMajor, role!!.term)
            }

            // node caught up
            if (member.nextIndex >= context.log()!!.nextIndex) {
                member.stopReplicating()
                return
            }
        } else {

            // backoff next index if failed to append entries
            if (!member.backOffNextIndex()) {
                logger.warn("cannot back off next index more, node {}", sourceNodeId)
                member.stopReplicating()
                return
            }
        }

        // replicate log to node immediately other than wait for next log replication
        doReplicateLog(member, context.config()!!.maxReplicationEntries)
    }

    /**
     * Receive install snapshot rpc.
     *
     * @param rpcMessage rpc message
     */
    @Subscribe
    fun onReceiveInstallSnapshotRpc(rpcMessage: InstallSnapshotRpcMessage) {
        context.taskExecutor()!!.submit(
            { context.connector()!!.replyInstallSnapshot(doProcessInstallSnapshotRpc(rpcMessage), rpcMessage) },
            LOGGING_FUTURE_CALLBACK
        )
    }

    private fun doProcessInstallSnapshotRpc(rpcMessage: InstallSnapshotRpcMessage): InstallSnapshotResult {
        val rpc = rpcMessage.get()

        // reply current term if term in rpc is smaller than current term
        if (rpc!!.term < role!!.term) {
            return InstallSnapshotResult(role!!.term)
        }

        // step down if term in rpc is larger than current one
        if (rpc.term > role!!.term) {
            becomeFollower(rpc.term, null, rpc.getLeaderId(), true)
        }
        val state: InstallSnapshotState = context.log()!!.installSnapshot(rpc)!!
        if (state.stateName === InstallSnapshotState.StateName.INSTALLED) {
            context.group()!!.updateNodes(state.lastConfig!!)
        }
        // TODO role check?
        return InstallSnapshotResult(rpc.term)
    }

    /**
     * Receive install snapshot result.
     *
     * @param resultMessage result message
     */
    @Subscribe
    fun onReceiveInstallSnapshotResult(resultMessage: InstallSnapshotResultMessage) {
        context.taskExecutor()!!.submit(
            { doProcessInstallSnapshotResult(resultMessage) },
            LOGGING_FUTURE_CALLBACK
        )
    }

    private fun doProcessInstallSnapshotResult(resultMessage: InstallSnapshotResultMessage) {
        val result = resultMessage.get()

        // step down if result's term is larger than current one
        if (result.term > role!!.term) {
            becomeFollower(result.term, null, null, true)
            return
        }

        // check role
        if (role!!.getName() !== RoleName.LEADER) {
            logger.warn(
                "receive install snapshot result from node {} but current node is not leader, ignore",
                resultMessage.getSourceNodeId()
            )
            return
        }

        // dispatch to new node catch up task by node id
        if (newNodeCatchUpTaskGroup.onReceiveInstallSnapshotResult(resultMessage, context.log()!!.nextIndex)) {
            return
        }
        val sourceNodeId = resultMessage.getSourceNodeId()
        val member: GroupMember = context.group()!!.getMember(sourceNodeId)!!
        val rpc: InstallSnapshotRpc = resultMessage.rpc
        if (rpc.isDone) {

            // change to append entries rpc
            member.advanceReplicatingState(rpc.lastIndex)
            val maxEntries: Int =
                if (member.isMajor) context.config()!!.maxReplicationEntries else context.config()!!
                    .maxReplicationEntriesForNewNode
            doReplicateLog(member, maxEntries)
        } else {

            // transfer data
            val nextRpc: InstallSnapshotRpc = context.log()!!.createInstallSnapshotRpc(
                role!!.term, context.selfId(),
                rpc.offset + rpc.getDataLength(), context.config()!!.snapshotDataLength
            )!!
            context.connector()!!.sendInstallSnapshot(nextRpc, member.endpoint)
        }
    }

    /**
     * Group config from leader appended.
     *
     *
     * Source: log.
     *
     *
     * @param event event
     */
    @Subscribe
    fun onGroupConfigEntryFromLeaderAppend(event: GroupConfigEntryFromLeaderAppendEvent) {
        context.taskExecutor()!!.submit({
            val entry: GroupConfigEntry = event.entry!!
            if (entry.kind == Entry.KIND_REMOVE_NODE &&
                context.selfId()!!.equals((entry as RemoveNodeEntry).getNodeToRemove())
            ) {
                logger.info("current node is removed from group, step down and standby")
                becomeFollower(role!!.term, null, null, false)
            }
            context.group()!!.updateNodes(entry.resultNodeEndpoints as Set<NodeEndpoint>)
        }, LOGGING_FUTURE_CALLBACK)
    }

    /**
     * Group config entry committed.
     *
     *
     * Source: log.
     *
     *
     * @param event event
     */
    @Subscribe
    fun onGroupConfigEntryCommitted(event: GroupConfigEntryCommittedEvent) {
        context.taskExecutor()!!.submit(
            { doProcessGroupConfigEntryCommittedEvent(event) },
            LOGGING_FUTURE_CALLBACK
        )
    }

    private fun doProcessGroupConfigEntryCommittedEvent(event: GroupConfigEntryCommittedEvent) {
        val entry: GroupConfigEntry = event.entry!!

        // dispatch to group config change task by node id
        groupConfigChangeTaskHolder.onLogCommitted(entry)
    }

    /**
     * Multiple group configs removed.
     *
     *
     * Source: log.
     *
     *
     * @param event event
     */
    @Subscribe
    fun onGroupConfigEntryBatchRemoved(event: GroupConfigEntryBatchRemovedEvent) {
        context.taskExecutor()!!.submit({
            val entry: GroupConfigEntry = event.getFirstRemovedEntry()
            context.group()!!.updateNodes(entry.getNodeEndpoints())
        }, LOGGING_FUTURE_CALLBACK)
    }

    /**
     * Generate snapshot.
     *
     *
     * Source: log.
     *
     *
     * @param event event
     */
    @Subscribe
    fun onGenerateSnapshot(event: SnapshotGenerateEvent) {
        context.taskExecutor()!!.submit({
            context.log()!!.generateSnapshot(event.lastIncludedIndex, context.group()!!.listEndpointOfMajor())
        }, LOGGING_FUTURE_CALLBACK)
    }

    /**
     * Dead event.
     *
     *
     * Source: event-bus.
     *
     *
     * @param deadEvent dead event
     */
    @Subscribe
    fun onReceiveDeadEvent(deadEvent: DeadEvent?) {
        logger.warn("dead event {}", deadEvent)
    }

    @Synchronized
    @Throws(InterruptedException::class)
    override fun stop() {
        check(started) { "node not started" }
        context.scheduler()!!.stop()
        context.log()!!.close()
        context.connector()!!.close()
        context.store()!!.close()
        context.taskExecutor()!!.shutdown()
        context.groupConfigChangeTaskExecutor()!!.shutdown()
        started = false
    }

    private inner class NewNodeCatchUpTaskContextImpl : NewNodeCatchUpTaskContext {
        override fun replicateLog(endpoint: NodeEndpoint?) {
            context.taskExecutor()!!.submit(
                { doReplicateLog(endpoint, context.log()!!.nextIndex) },
                LOGGING_FUTURE_CALLBACK
            )
        }

        override fun doReplicateLog(endpoint: NodeEndpoint?, nextIndex: Int) {
            try {
                val rpc: AppendEntriesRpc = context.log()!!.createAppendEntriesRpc(
                    role!!.term,
                    context.selfId(),
                    nextIndex,
                    context.config()!!.maxReplicationEntriesForNewNode
                )!!
                if (endpoint != null) {
                    context.connector()!!.sendAppendEntries(rpc, endpoint)
                }
            } catch (ignored: EntryInSnapshotException) {

                // change to install snapshot rpc if entry in snapshot
                logger.debug("log entry {} in snapshot, replicate with install snapshot RPC", nextIndex)
                val rpc: InstallSnapshotRpc = context.log()!!.createInstallSnapshotRpc(
                    role!!.term,
                    context.selfId(),
                    0,
                    context.config()!!.snapshotDataLength
                )!!
                if (endpoint != null) {
                    context.connector()!!.sendInstallSnapshot(rpc, endpoint)
                }
            }
        }

        override fun sendInstallSnapshot(endpoint: NodeEndpoint?, offset: Int) {
            val rpc: InstallSnapshotRpc = context.log()!!.createInstallSnapshotRpc(
                role!!.term,
                context.selfId(),
                offset,
                context.config()!!.snapshotDataLength
            )!!
            if (endpoint != null) {
                context.connector()!!.sendInstallSnapshot(rpc, endpoint)
            }
        }

        override fun done(task: NewNodeCatchUpTask?) {

            // remove task from group
            newNodeCatchUpTaskGroup.remove(task!!)
        }
    }

    private inner class GroupConfigChangeTaskContextImpl : GroupConfigChangeTaskContext {
        override fun addNode(endpoint: NodeEndpoint?, nextIndex: Int, matchIndex: Int) {
            context.taskExecutor()!!.submit({
                context.log()!!.appendEntryForAddNode(role!!.term, context.group()!!.listEndpointOfMajor(), endpoint)
                assert(!context.selfId()!!.equals(endpoint!!.id))
                context.group()!!.addNode(endpoint, nextIndex, matchIndex, true)
                this@NodeImpl.doReplicateLog()
            }, LOGGING_FUTURE_CALLBACK)
        }

        override fun downgradeNode(nodeId: NodeId?) {
            context.taskExecutor()!!.submit({
                if (nodeId != null) {
                    context.group()!!.downgrade(nodeId)
                }
                val nodeEndpoints: Set<NodeEndpoint> = context.group()!!.listEndpointOfMajor()
                role?.term?.let { context.log()?.appendEntryForRemoveNode(it, nodeEndpoints, nodeId) }
                this@NodeImpl.doReplicateLog()
            }, LOGGING_FUTURE_CALLBACK)
        }

        override fun removeNode(nodeId: NodeId?) {
            context.taskExecutor()!!.submit({
                if (nodeId != null) {
                    if (nodeId.equals(context.selfId())) {
                        logger.info("remove self from group, step down and standby")
                        role?.term?.let { becomeFollower(it, null, null, false) }
                    }
                }
                if (nodeId != null) {
                    context.group()?.removeNode(nodeId)
                }
            }, LOGGING_FUTURE_CALLBACK)
        }

        override fun done() {

            // clear current group config change
            synchronized(this@NodeImpl) { groupConfigChangeTaskHolder = GroupConfigChangeTaskHolder() }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeImpl::class.java)

        // callback for async tasks.
        private val LOGGING_FUTURE_CALLBACK: FutureCallback<Any?> = object : FutureCallback<Any?> {
            override fun onSuccess(result: Any?) {}
            override fun onFailure(t: Throwable) {
                logger.warn("failure", t)
            }
        }
    }
}

