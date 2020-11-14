package raft.node.task

import raft.node.NodeEndpoint


/**
 * Task context for [NewNodeCatchUpTask].
 */
interface NewNodeCatchUpTaskContext {
    /**
     * Replicate log to new node.
     *
     *
     * Process will be run in node task executor.
     *
     *
     * @param endpoint endpoint
     */
    fun replicateLog(endpoint: NodeEndpoint?)

    /**
     * Replicate log to endpoint.
     *
     * @param endpoint  endpoint
     * @param nextIndex next index
     */
    fun doReplicateLog(endpoint: NodeEndpoint?, nextIndex: Int)
    fun sendInstallSnapshot(endpoint: NodeEndpoint?, offset: Int)

    /**
     * Done and remove current task.
     *
     * @param task task
     */
    fun done(task: NewNodeCatchUpTask?)
}


