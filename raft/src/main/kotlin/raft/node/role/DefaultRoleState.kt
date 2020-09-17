package raft.node.role

import raft.node.NodeId.NodeId
import raft.node.RoleName
import javax.annotation.Nonnull


/**
 * Default role state.
 */
class DefaultRoleState(roleName: RoleName, term: Int) : RoleState {
    private val roleName: RoleName = roleName
    private val term: Int = term
    private var votesCount = VOTES_COUNT_NOT_SET
    private var votedFor: NodeId? = null
    private var leaderId: NodeId? = null

    @Nonnull
    override fun getRoleName(): RoleName {
        return roleName
    }

    override fun getTerm(): Int {
        return term
    }

    override fun getVotesCount(): Int {
        return votesCount
    }

    fun setVotesCount(votesCount: Int) {
        this.votesCount = votesCount
    }

    override fun getVotedFor(): NodeId? {
        return votedFor
    }

    fun setVotedFor(votedFor: NodeId?) {
        this.votedFor = votedFor
    }

    override fun getLeaderId(): NodeId? {
        return leaderId
    }

    fun setLeaderId(leaderId: NodeId?) {
        this.leaderId = leaderId
    }

    override fun toString(): String {
        return when (roleName) {
            RoleName.FOLLOWER -> "Follower{term=$term, votedFor=$votedFor, leaderId=$leaderId}"
            RoleName.CANDIDATE -> "Candidate{term=$term, votesCount=$votesCount}"
            RoleName.LEADER -> "Leader{term=$term}"
            else -> throw IllegalStateException("unexpected node role name [$roleName]")
        }
    }

}


