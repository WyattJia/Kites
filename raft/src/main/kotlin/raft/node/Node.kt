package raft.node

import raft.node.role.RoleNameAndLeaderId

/**
 * Node.
 */
interface Node {
    /**
     * Register state machine to node.
     *
     * State machine should be registered before node start, or it may not take effect.
     *
     * @param stateMachine state machine
     */
    fun registerStateMachine(stateMachine: StateMachineImpl)

    /**
     * Get current role name and leader id.
     *
     *
     * Available results:
     *
     *
     *  * FOLLOWER, current leader id
     *  * CANDIDATE, `null`
     *  * LEADER, self id
     *
     *
     * @return role name and leader id
     */
    val roleNameAndLeaderId: RoleNameAndLeaderId?

    /**
     * Add node role listener.
     *
     * @param listener listener
     */
//    fun addNodeRoleListener(listener: NodeRoleListener?)

    /**
     * Start node.
     */
    fun start()

    /**
     * Append log.
     *
     * @param commandBytes command bytes
     * @throws NotLeaderException if not leader
     */
    fun appendLog(commandBytes: ByteArray?)

    /**
     * Add node.
     *
     * @param endpoint new node endpoint
     * @return task reference
     * @throws NotLeaderException if not leader
     * @throws IllegalStateException if group config change concurrently
     */
//    fun addNode(endpoint: NodeEndpoint?): GroupConfigChangeTaskReference?

    /**
     * Remove node.
     *
     * @param id id
     * @return task reference
     * @throws NotLeaderException if not leader
     * @throws IllegalStateException if group config change concurrently
     */
//    fun removeNode(@Nonnull id: NodeId?): GroupConfigChangeTaskReference?

    /**
     * Stop node.
     *
     * @throws InterruptedException if interrupted
     */
    @Throws(InterruptedException::class)
    fun stop()
}
