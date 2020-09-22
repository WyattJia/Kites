package raft.node.ReplicatingState

/**
 * Replicating state.
 */
class ReplicatingState (
    /**
     * Get next index.
     *
     * @return next index
     */
    var nextIndex: Int,
    /**
     * Get match index.
     *
     * @return match index
     */
    var matchIndex: Int = 0
) {
    /**
     * Test if replicating.
     *
     * @return true if replicating, otherwise false
     */

    /**
     * Set replicating.
     *
     * @param replicating replicating
     */
    var isReplicating = false
    /**
     * Get last replicated timestamp.
     *
     * @return last replicated timestamp
     */
    /**
     * Set last replicated timestamp.
     *
     * @param lastReplicatedAt last replicated timestamp
     */
    var lastReplicatedAt: Long = 0

    /**
     * Back off next index, in other word, decrease.
     *
     * @return true if decrease successfully, false if next index is less than or equal to `1`
     */
    fun backOffNextIndex(): Boolean {
        if (nextIndex > 1) {
            nextIndex--
            return true
        }
        return false
    }

    /**
     * Advance next index and match index by last entry index.
     *
     * @param lastEntryIndex last entry index
     * @return true if advanced, false if no change
     */
    fun advance(lastEntryIndex: Int): Boolean {
        // changed
        val result = matchIndex != lastEntryIndex || nextIndex != lastEntryIndex + 1
        matchIndex = lastEntryIndex
        nextIndex = lastEntryIndex + 1
        return result
    }

    override fun toString(): String {
        return "ReplicatingState{" +
                "nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                ", replicating=" + isReplicating +
                ", lastReplicatedAt=" + lastReplicatedAt +
                '}'
    }
}


