package raft.node.task

import org.slf4j.LoggerFactory
import raft.node.NodeEndpoint
import raft.node.NodeId
import raft.node.config.NodeConfig
import raft.rpc.message.AppendEntriesResultMessage
import raft.rpc.message.InstallSnapshotResultMessage
import raft.rpc.message.InstallSnapshotRpc
import java.util.concurrent.Callable

class NewNodeCatchUpTask(context: NewNodeCatchUpTaskContext, endpoint: NodeEndpoint, config: NodeConfig) :
    Callable<NewNodeCatchUpTaskResult> {
    private enum class State {
        START, REPLICATING, REPLICATION_FAILED, REPLICATION_CATCH_UP, TIMEOUT
    }

    private val context: NewNodeCatchUpTaskContext
    private val endpoint: NodeEndpoint
    private val nodeId: NodeId
    private val config: NodeConfig
    private var state = State.START
    private var done = false
    private var lastReplicateAt // set when start
            : Long = 0
    private var lastAdvanceAt // set when start
            : Long = 0
    private var round = 1
    private var nextIndex = 0 // reset when receive append entries result
    private var matchIndex = 0
    fun getNodeId(): NodeId {
        return nodeId
    }

    @Synchronized
    @Throws(Exception::class)
    override fun call(): NewNodeCatchUpTaskResult {
        logger.debug("task start")
        setState(State.START)
        context.replicateLog(endpoint)
        lastReplicateAt = System.currentTimeMillis()
        lastAdvanceAt = lastReplicateAt
        setState(State.REPLICATING)
        while (!done) {
            // todo use coroutine
            Object().wait(config.newNodeReadTimeout.toLong())
            // 1. done
            // 2. replicate -> no response within timeout
            if (System.currentTimeMillis() - lastReplicateAt >= config.newNodeReadTimeout) {
                logger.debug("node {} not response within read timeout", endpoint.id)
                state = State.TIMEOUT
                break
            }
        }
        logger.debug("task done")
        context.done(this)
        return mapResult(state)
    }

    private fun mapResult(state: State): NewNodeCatchUpTaskResult {
        return when (state) {
            State.REPLICATION_CATCH_UP -> NewNodeCatchUpTaskResult(
                nextIndex,
                matchIndex
            )
            State.REPLICATION_FAILED -> NewNodeCatchUpTaskResult(NewNodeCatchUpTaskResult.State.REPLICATION_FAILED)
            else -> NewNodeCatchUpTaskResult(NewNodeCatchUpTaskResult.State.TIMEOUT)
        }
    }

    private fun setState(state: State) {
        logger.debug("state -> {}", state)
        this.state = state
    }

    // in node thread
    @Synchronized
    fun onReceiveAppendEntriesResult(resultMessage: AppendEntriesResultMessage, nextLogIndex: Int) {
        assert(nodeId.equals(resultMessage.getSourceNodeId()))
        check(state == State.REPLICATING) { "receive append entries result when state is not replicating" }
        // initialize nextIndex
        if (nextIndex == 0) {
            nextIndex = nextLogIndex
        }
        logger.debug("replication state of new node {}, next index {}, match index {}", nodeId, nextIndex, matchIndex)
        if (resultMessage.get().isSuccess()) {
            val lastEntryIndex: Int = resultMessage.rpc.getLastEntryIndex()
            assert(lastEntryIndex >= 0)
            matchIndex = lastEntryIndex
            nextIndex = lastEntryIndex + 1
            lastAdvanceAt = System.currentTimeMillis()
            if (nextIndex >= nextLogIndex) { // catch up
                setStateAndNotify(State.REPLICATION_CATCH_UP)
                return
            }
            if (++round > config.newNodeMaxRound) {
                logger.info("node {} cannot catch up within max round", nodeId)
                setStateAndNotify(State.TIMEOUT)
                return
            }
        } else {
            if (nextIndex <= 1) {
                logger.warn("node {} cannot back off next index more, stop replication", nodeId)
                setStateAndNotify(State.REPLICATION_FAILED)
                return
            }
            nextIndex--
            if (System.currentTimeMillis() - lastAdvanceAt >= config.newNodeAdvanceTimeout) {
                logger.debug("node {} cannot make progress within timeout", nodeId)
                setStateAndNotify(State.TIMEOUT)
                return
            }
        }
        context.doReplicateLog(endpoint, nextIndex)
        lastReplicateAt = System.currentTimeMillis()
        // todo use coroutine
        Object().notify()
    }

    // in node thread
    @Synchronized
    fun onReceiveInstallSnapshotResult(resultMessage: InstallSnapshotResultMessage, nextLogIndex: Int) {
        assert(nodeId.equals(resultMessage.getSourceNodeId()))
        check(state == State.REPLICATING) { "receive append entries result when state is not replicating" }
        val rpc: InstallSnapshotRpc = resultMessage.rpc
        if (rpc.isDone) {
            matchIndex = rpc.lastIndex
            nextIndex = rpc.lastIndex + 1
            lastAdvanceAt = System.currentTimeMillis()
            if (nextIndex >= nextLogIndex) {
                setStateAndNotify(State.REPLICATION_CATCH_UP)
                return
            }
            round++
            context.doReplicateLog(endpoint, nextIndex)
        } else {
            context.sendInstallSnapshot(endpoint, rpc.offset + rpc.getDataLength())
        }
        lastReplicateAt = System.currentTimeMillis()
        // todo
        Object().notify()
    }

    private fun setStateAndNotify(state: State) {
        setState(state)
        done = true
        // todo
        Object().notify()
    }

    override fun toString(): String {
        return "NewNodeCatchUpTask{" +
                "state=" + state +
                ", endpoint=" + endpoint +
                ", done=" + done +
                ", lastReplicateAt=" + lastReplicateAt +
                ", lastAdvanceAt=" + lastAdvanceAt +
                ", nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                ", round=" + round +
                '}'
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NewNodeCatchUpTask::class.java)
    }

    init {
        this.context = context
        this.endpoint = endpoint
        nodeId = endpoint.id
        this.config = config
    }
}