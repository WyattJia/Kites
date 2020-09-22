package raft.node.role

import raft.node.NodeId
import raft.node.RoleName
import javax.annotation.Nonnull

interface RoleState {
    /**
     * Get role name.
     *
     * @return role name
     */
    @get:Nonnull
    val roleName: RoleName?

    /**
     * Get term.
     *
     * @return term
     */
    val term: Int

    /**
     * Get votes count.
     *
     * @return votes count, {@value VOTES_COUNT_NOT_SET} if unknown
     */
    val votesCount: Int

    /**
     * Get voted for.
     *
     * @return voted for
     */
    val votedFor: NodeId?

    /**
     * Get leader id.
     *
     * @return leader id
     */
    val leaderId: NodeId?

    companion object {
        const val VOTES_COUNT_NOT_SET = -1
    }
}
