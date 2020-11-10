package raft.node

import com.google.common.base.Preconditions
import com.google.common.eventbus.Subscribe
import com.google.common.util.concurrent.FutureCallback
import raft.log.entry.EntryMeta
import raft.node.role.CandidateNodeRole
import raft.node.role.FollowerNodeRole
import raft.node.role.LeaderNodeRole
import raft.node.role.RoleNameAndLeaderId
import raft.rpc.message.*
import raft.schedule.ElectionTimeout
import raft.schedule.LogReplicationTask
import raft.support.Log
import javax.annotation.Nonnull
import kotlin.properties.Delegates

class NodeImpl(val context: NodeContext) : Node {

    companion object : Log {}

    var started by Delegates.notNull<Boolean>()

    // current node role
    lateinit var role: AbstractNodeRole

    @Synchronized
    override fun start() {
        if (this.started) {
            return
        }

        context.eventbus.register(this)
        context.connector.initialize()

        val store = context.store
        scheduleElectionTimeout()?.let {
            FollowerNodeRole(
                store.getTerm(),
                store.getVotedFor(),
                null,
                it
            )
        }?.let {
            changeToRole(
                it
            )
        }


        started = true

    }

    @Synchronized
    @Throws(InterruptedException::class)
    override fun stop() {
        check(started) { "node not started" }
        context.scheduler.stop()
        context.log.close()
        context.connector.close()
        context.store.close()
        context.taskExecutor.shutdown()
        context.groupConfigChangeTaskExecutor.shutdown()
        started = false
    }


    private fun scheduleElectionTimeout(): ElectionTimeout? {
        return context.scheduler.scheduleElectionTimeout(this::electionTimeout)
    }

    fun electionTimeout() {
        context.taskExecutor.submit(this::doProcessElectionTimeout)
    }

    private fun doProcessElectionTimeout() {
        if (role.getName() === RoleName.LEADER) {
            logger().warn(
                "node {}, current role is leader, ignore election timeout",
                context.selfId
            )
            return
        }

        // follower: start election
        // candidate: restart election
        val newTerm: Int = role.term + 1
        role.cancelTimeoutOrTask()
        if (context.group.isStandalone()) {
            if (context.mode === NodeMode.STANDBY) {
                logger().info("starts with standby mode, skip election")
            } else {

                // become leader
                logger().info("become leader, term {}", newTerm)
                resetReplicatingStates()
                changeToRole(LeaderNodeRole(newTerm, scheduleLogReplicationTask()))
                context.log.appendEntry(newTerm) // no-op log
            }
        } else {
            logger().info("start election")
            changeToRole(CandidateNodeRole(newTerm, scheduleElectionTimeout()!!))

            // request vote
            val lastEntryMeta: EntryMeta = context.log.getLastEntryMeta()
            val rpc = RequestVoteRpc()
            rpc.term = newTerm
            rpc.candidateId = context.selfId
            rpc.lastLogIndex = lastEntryMeta.index
            rpc.lastLogTerm = lastEntryMeta.term
            context.connector.sendRequestVote(rpc, context.group.listEndpointOfMajorExceptSelf())
        }
    }


    @Synchronized
    override fun registerStateMachine(stateMachine: Any) {
        Preconditions.checkNotNull<Any>(stateMachine)
        context.log.setStateMachine(stateMachine)
    }

    /**
     * Ensure leader status
     *
     * @throws NotLeaderException if not leader
     */
    private fun ensureLeader() {
        val result: RoleNameAndLeaderId? = role.getNameAndLeaderId(context.selfId)
        if (result != null) {
            if (result.roleName == RoleName.LEADER) {
                return
            }
        }
        val endpoint: NodeEndpoint? =
            if (result?.getLeaderId() != null) context.group.findMember(result.leaderId!!).endpoint else null
        if (result != null) {
            throw NotLeaderException(result.getRoleName(), endpoint)
        }
    }


    /**
     * Append log.
     *
     * @param commandBytes command bytes
     * @throws NotLeaderException if not leader
     */
    override fun appendLog(commandBytes: ByteArray?) {
        Preconditions.checkNotNull(commandBytes)
        ensureLeader()
        context.taskExecutor.submit({
            if (commandBytes != null) {
                context.log.appendEntry(role.term, commandBytes)
            }
            doReplicateLog()
        }, LOGGING_FUTURE_CALLBACK)
    }

    fun replicateLog() {
        context.taskExecutor.submit(this::doReplicateLog)
    }

    private fun doReplicateLog() {
        // just advance commit index if is unique node
        logger().debug("replicate log")
        for (member in context.group.listReplicationTarget()!!) {
            doReplicateLog(member as GroupMember)
        }
    }

    private fun doReplicateLog(member: GroupMember) {
        val rpc: AppendEntriesRpc = AppendEntriesRpc()
        rpc.term = role.term
        rpc.leaderId = context.selfId
        rpc.prevLogIndex = 0
        rpc.prevLogTerm = 0
        rpc.leaderCommit = 0
        context.connector.sendAppendEntries(rpc, member.endpoint)
    }

    private fun changeToRole(newRole: AbstractNodeRole) {
        if (!isStableBetween(role, newRole)) {
            logger().debug("Node {}, role state changed -> {}", context.selfId, newRole)

            val state = newRole.state
            val store = context.store

            if (state != null) {
                store.setTerm(state.term)
            }

            if (state != null) {
                state.votedFor?.let { store.setVotedFor(it) }
            }
        }
    }

    private val LOGGING_FUTURE_CALLBACK: FutureCallback<Any?> = object : FutureCallback<Any?> {
        override fun onSuccess(result: Any?) {}
        override fun onFailure(@Nonnull t: Throwable) {
            logger().warn("failure", t)
        }
    }

    private fun isStableBetween(before: AbstractNodeRole, after: AbstractNodeRole): Boolean {
        return before.stateEquals(after)
    }

    @Subscribe
    fun onReceiveRequestVoteRpc(rpcMessage: RequestVoteRpcMessage?) {
        context.taskExecutor.submit(
            { context.connector.replyRequestVote(doProcessRequestVoteRpc(rpcMessage!!)!!, rpcMessage) },
            LOGGING_FUTURE_CALLBACK
        )
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
    fun onReceiveRequestVoteResult(result: RequestVoteResult?) {
        context.taskExecutor.submit({
            if (result != null) {
                doProcessRequestVoteResult(result)
            }
        }, LOGGING_FUTURE_CALLBACK)
    }

    private fun doProcessRequestVoteResult(result: RequestVoteResult) {

        // step down if result's term is larger than current term
        if (result.getTerm() > role.term) {
            becomeFollower(result.getTerm(), null, null, true)
            return
        }

        // check role
        if (role.getName() !== RoleName.CANDIDATE) {
            logger().debug("receive request vote result and current role is not candidate, ignore")
            return
        }

        // do nothing if not vote granted
        if (!result.isVoteGranted()) {
            return
        }
        val currentVotesCount: Int = (role as CandidateNodeRole).votesCount + 1
        val countOfMajor: Int = context.group.countOfMajor
        logger().debug("votes count {}, major node count {}", currentVotesCount, countOfMajor)
        role.cancelTimeoutOrTask()
        if (currentVotesCount > countOfMajor / 2) {

            // become leader
            logger().info("become leader, term {}", role.term)
            resetReplicatingStates()
            changeToRole(LeaderNodeRole(role.term, scheduleLogReplicationTask()))
            context.log.appendEntry(role.term) // no-op log
            context.connector.resetChannels()// close all inbound channels
        } else {

            // update votes count
            changeToRole(CandidateNodeRole(role.term, currentVotesCount, scheduleElectionTimeout()!!))
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
        context.taskExecutor.submit(
            {
                doProcessAppendEntriesRpc(rpcMessage)?.let {
                    context.connector.replyAppendEntries(
                        it,
                        rpcMessage
                    )
                }
            },
            LOGGING_FUTURE_CALLBACK
        )
    }


    /**
     * Append entries and advance commit index if possible.
     *
     * @param rpc rpc
     * @return `true` if log appended, `false` if previous log check failed, etc
     */
    private fun appendEntries(rpc: AppendEntriesRpc): Boolean {
        val result: Boolean =
            context.log.appendEntriesFromLeader(rpc.prevLogIndex, rpc.prevLogTerm, rpc.entries)
        if (result) {
            context.log.advanceCommitIndex(Math.min(rpc.leaderCommit, rpc.getLastEntryIndex()), rpc.term)
        }
        return result
    }


    private fun doProcessAppendEntriesRpc(rpcMessage: AppendEntriesRpcMessage): AppendEntriesResult {
        val rpc = rpcMessage.get()

        // reply current term if term in rpc is smaller than current term
        if (rpc != null) {
            if (rpc.term < role.term) {
                return AppendEntriesResult(role.term, false)
            }
        }

        // if term in rpc is larger than current term, step down and append entries
        if (rpc != null) {
            if (rpc.term > role.term) {
                becomeFollower(rpc.term, null, rpc.leaderId, true)
                return AppendEntriesResult(rpc.term, appendEntries(rpc))
            }
        }
        if (rpc != null) {
            return when (role.getName()) {
                RoleName.FOLLOWER -> {

                    // reset election timeout and append entries
                    becomeFollower(rpc.term, (role as FollowerNodeRole).votedFor, rpc.leaderId, true)
                    AppendEntriesResult(rpc.term, appendEntries(rpc))
                }
                RoleName.CANDIDATE -> {

                    // more than one candidate but another node won the election
                    becomeFollower(rpc.term, null, rpc.leaderId, true)
                    AppendEntriesResult(rpc.term, appendEntries(rpc))
                }
                RoleName.LEADER -> {
                    logger().warn("receive append entries rpc from another leader {}, ignore", rpc.leaderId)
                    AppendEntriesResult(rpc.term, false)
                }
            }
        } else {
            throw IllegalStateException("unexpected node role [" + role.getName().toString() + "]")

        }
    }


    /**
     * Reset replicating states.
     */
    private fun resetReplicatingStates() {
        context.group.resetReplicatingStates(context.log.nextIndex)
    }


    /**
     * Schedule log replication task.
     *
     * @return log replication task
     */
    private fun scheduleLogReplicationTask(): LogReplicationTask {
        return context.scheduler.scheduleLogReplicationTask { replicateLog() }
    }

    private fun doProcessRequestVoteRpc(rpcMessage: RequestVoteRpcMessage): RequestVoteResult? {

        // skip non-major node, it maybe removed node
        if (!context.group.isMemberOfMajor(rpcMessage.sourceNodeId)) {
            logger().warn(
                "receive request vote rpc from node {} which is not major node, ignore",
                rpcMessage.sourceNodeId
            )
            return RequestVoteResult(role.term, false)
        }

        // reply current term if result's term is smaller than current one
        val rpc = rpcMessage.get()
        if (rpc != null) {
            if (rpc.term < role.term) {
                logger().debug(
                    "term from rpc < current term, don't vote (${rpc.term} < ${role.term})"
                )
                return RequestVoteResult(role.term, false)
            }
        }

        // step down if result's term is larger than current term
        if (rpc != null) {
            if (rpc.term > role.term) {
                val voteForCandidate: Boolean = !context.log.isNewerThan(rpc.lastLogIndex, rpc.lastLogTerm)
                becomeFollower(rpc.term, if (voteForCandidate) rpc.candidateId else null, null, true)
                return RequestVoteResult(rpc.term, voteForCandidate)
            }
        }
        if (rpc != null) {
            assert(rpc.term == role.term)
        }
        return when (role.getName()) {
            RoleName.FOLLOWER -> {
                val follower = role as FollowerNodeRole
                val votedFor: NodeId = follower.votedFor
                // reply vote granted for
                // 1. not voted and candidate's log is newer than self
                // 2. voted for candidate
                if (rpc != null) {
                    if (votedFor == rpc.candidateId
                    ) {
                        becomeFollower(role.term, rpc.candidateId, null, true)
                        return RequestVoteResult(rpc.term, true)
                    }
                }
                RequestVoteResult(role.term, false)
            }
            RoleName.CANDIDATE, RoleName.LEADER -> RequestVoteResult(role.term, false)
        }
    }

    @Subscribe
    fun onReceiveAppendEntriesResult(resultMessage: AppendEntriesResultMessage) {
        context.taskExecutor.submit({ doProcessAppendEntriesResult(resultMessage) }, LOGGING_FUTURE_CALLBACK)
    }

    private fun doProcessAppendEntriesResult(resultMessage: AppendEntriesResultMessage) {
        val result = resultMessage.get()

        // step down if result's term is larger than current term
        if (result.getTerm() > role.term) {
            becomeFollower(result.getTerm(), null, null, true)
            return
        }

        // check role
        if (role.getName() !== RoleName.LEADER) {
            logger().warn(
                "receive append entries result from node {} but current node is not leader, ignore",
                resultMessage.getSourceNodeId()
            )
            return
        }

        val sourceNodeId = resultMessage.getSourceNodeId()
        val member: GroupMember? = context.group.getMember(sourceNodeId)
        if (member == null) {
            logger().info("unexpected append entries result from node {}, node maybe removed", sourceNodeId)
            return
        }
        val rpc: AppendEntriesRpc = resultMessage.rpc
        if (result.isSuccess()) {
            if (!member.isMajor) {  // removing node
                if (member.advanceReplicatingState(rpc.getLastEntryIndex())) {
                    context.log.advanceCommitIndex(
                        context.group.matchIndexOfMajor, role.term
                    )
                } else {
                    logger().warn(
                        "unexpected append entries result from node {}, not major and not removing",
                        sourceNodeId
                    )
                }
                member.stopReplicating()
                return
            }

            // node caught up
            if (member.nextIndex >= context.log.nextIndex) {
                member.stopReplicating()
                return
            }
        } else {

            // backoff next index if failed to append entries
            if (!member.backOffNextIndex()) {
                logger().warn("cannot back off next index more, node {}", sourceNodeId)
                member.stopReplicating()
                return
            }
        }

    }

    private fun becomeFollower(term: Int, votedFor: NodeId?, leaderId: NodeId?, scheduleElectionTimeout: Boolean) {
        role.cancelTimeoutOrTask()
        if (leaderId != null && !leaderId.equals(role.getLeaderId(context.selfId))) {
            logger().info("current leader is {}, term {}", leaderId, term)
        }
        val electionTimeout = if (scheduleElectionTimeout) scheduleElectionTimeout() else ElectionTimeout.NONE
        electionTimeout?.let { votedFor?.let { it1 -> FollowerNodeRole(term, it1, leaderId, it) } }?.let {
            changeToRole(
                it
            )
        }
    }
}

