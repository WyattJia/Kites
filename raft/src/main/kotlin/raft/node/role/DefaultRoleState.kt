package raft.node.role

import raft.node.NodeId
import raft.node.RoleName
import raft.node.role.RoleState.Companion.VOTES_COUNT_NOT_SET
import javax.annotation.Nonnull

/**
 * Default role state.
 */
class DefaultRoleState(@get:Nonnull override val roleName: RoleName, override val term: Int) : RoleState {
    override var votesCount: Int = VOTES_COUNT_NOT_SET
    override var votedFor: NodeId? = null
    override var leaderId: NodeId? = null

    override fun toString(): String {
        return when (roleName) {
            RoleName.FOLLOWER -> "Follower{term=$term, votedFor=$votedFor, leaderId=$leaderId}"
            RoleName.CANDIDATE -> "Candidate{term=$term, votesCount=$votesCount}"
            RoleName.LEADER -> "Leader{term=$term}"
            else -> throw IllegalStateException("unexpected node role name [$roleName]")
        }
    }
}

