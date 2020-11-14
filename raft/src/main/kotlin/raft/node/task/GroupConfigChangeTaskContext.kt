package raft.node.task

import raft.node.NodeEndpoint
import raft.node.NodeId


/**
 * Task context for [GroupConfigChangeTask].
 */
interface GroupConfigChangeTaskContext {
    /**
     * Add node.
     *
     *
     * Process will be run in node task executor.
     *
     *
     *  * add node to group
     *  * append log entry
     *  * replicate
     *
     *
     * @param endpoint   endpoint
     * @param nextIndex  next index
     * @param matchIndex match index
     */
    fun addNode(endpoint: NodeEndpoint?, nextIndex: Int, matchIndex: Int)

    /**
     * Downgrade node.
     *
     *
     * Process will be run in node task executor.
     *
     *
     *  * downgrade node
     *  * append log entry
     *  * replicate
     *
     *
     * @param nodeId node id to downgrade
     */
    fun downgradeNode(nodeId: NodeId?)

    /**
     * Remove node from group.
     *
     *
     * Process will be run in node task executor.
     *
     *
     *
     * if node id is self id, step down.
     *
     *
     * @param nodeId node id
     */
    fun removeNode(nodeId: NodeId?)

    /**
     * Done and remove current group config change task.
     */
    fun done()
}


