package raft.node.GroupMember


import raft.node.NodeEndpoint.NodeEndpoint
import raft.node.NodeId.NodeId
import raft.node.ReplicatingState.ReplicatingState


/**
 * State of group member.
 *
 * @see ReplicatingState
 */
class GroupMember(
    val endpoint: NodeEndpoint,
    private var replicatingState: ReplicatingState?,
    var isMajor: Boolean
) {
    var isRemoving = false
        private set

    constructor(endpoint: NodeEndpoint) : this(endpoint, null, true) {}

    val id: NodeId
        get() = endpoint.id

    fun idEquals(id: NodeId?): Boolean {
        return endpoint.id.equals(id)
    }

    fun setReplicatingState(replicatingState: ReplicatingState?) {
        this.replicatingState = replicatingState
    }

    val isReplicationStateSet: Boolean
        get() = replicatingState != null

    private fun ensureReplicatingState(): ReplicatingState {
        checkNotNull(replicatingState) { "replication state not set" }
        return replicatingState!!
    }

    fun setRemoving() {
        isRemoving = true
    }

    val nextIndex: Int
        get() = ensureReplicatingState().nextIndex
    val matchIndex: Int
        get() = ensureReplicatingState().matchIndex

    fun advanceReplicatingState(lastEntryIndex: Int): Boolean {
        return ensureReplicatingState().advance(lastEntryIndex)
    }

    fun backOffNextIndex(): Boolean {
        return ensureReplicatingState().backOffNextIndex()
    }

    fun replicateNow() {
        replicateAt(System.currentTimeMillis())
    }

    fun replicateAt(replicatedAt: Long) {
        val replicatingState = ensureReplicatingState()
        replicatingState.isReplicating = true
        replicatingState.lastReplicatedAt = replicatedAt
    }

    val isReplicating: Boolean
        get() = ensureReplicatingState().isReplicating

    fun stopReplicating() {
        ensureReplicatingState().isReplicating = false
    }

    /**
     * Test if should replicate.
     *
     *
     * Return true if
     *
     *  1. not replicating
     *  1. replicated but no response in specified timeout
     *
     *
     *
     * @param readTimeout read timeout
     * @return true if should, otherwise false
     */
    fun shouldReplicate(readTimeout: Long): Boolean {
        val replicatingState = ensureReplicatingState()
        return !replicatingState.isReplicating ||
                System.currentTimeMillis() - replicatingState.lastReplicatedAt >= readTimeout
    }

    override fun toString(): String {
        return "GroupMember{" +
                "endpoint=" + endpoint +
                ", major=" + isMajor +
                ", removing=" + isRemoving +
                ", replicatingState=" + replicatingState +
                '}'
    }
}

