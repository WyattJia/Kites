package raft.node.task

import raft.node.NodeId
import raft.rpc.message.AppendEntriesResultMessage
import raft.rpc.message.InstallSnapshotResultMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.annotation.concurrent.ThreadSafe

/**
 * Group for [NewNodeCatchUpTask].
 */
@ThreadSafe
class NewNodeCatchUpTaskGroup {
    private val taskMap: ConcurrentMap<NodeId, NewNodeCatchUpTask?> = ConcurrentHashMap<NodeId, NewNodeCatchUpTask?>()

    /**
     * Add task.
     *
     * @param task task
     * @return true if successfully, false if task for same node exists
     */
    fun add(task: NewNodeCatchUpTask): Boolean {
        return taskMap.putIfAbsent(task.getNodeId(), task) == null
    }

    /**
     * Invoke `onReceiveAppendEntriesResult` on task.
     *
     * @param resultMessage result message
     * @param nextLogIndex  next index of log
     * @return true if invoked, false if no task for node
     */
    fun onReceiveAppendEntriesResult(resultMessage: AppendEntriesResultMessage, nextLogIndex: Int): Boolean {
        val task: NewNodeCatchUpTask = taskMap[resultMessage.getSourceNodeId()] ?: return false
        task.onReceiveAppendEntriesResult(resultMessage, nextLogIndex)
        return true
    }

    fun onReceiveInstallSnapshotResult(resultMessage: InstallSnapshotResultMessage, nextLogIndex: Int): Boolean {
        val task: NewNodeCatchUpTask = taskMap[resultMessage.getSourceNodeId()] ?: return false
        task.onReceiveInstallSnapshotResult(resultMessage, nextLogIndex)
        return true
    }

    /**
     * Remove task.
     *
     * @param task task
     * @return `true` if removed, `false` if not found
     */
    fun remove(task: NewNodeCatchUpTask): Boolean {
        return taskMap.remove(task.getNodeId()) != null
    }
}