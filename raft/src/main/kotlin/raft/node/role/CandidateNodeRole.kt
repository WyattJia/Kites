package raft.node.role


import raft.node.AbstractNodeRole
import raft.node.NodeId
import raft.node.RoleName
import raft.schedule.ElectionTimeout
import javax.annotation.concurrent.Immutable

@Immutable
class CandidateNodeRole(term: Int, val votesCount: Int, electionTimeout: ElectionTimeout) :
        AbstractNodeRole(RoleName.CANDIDATE, term) {
    private val electionTimeout: ElectionTimeout = electionTimeout

    constructor(term: Int, electionTimeout: ElectionTimeout) : this(term, 1, electionTimeout) {}

    override fun getLeaderId(selfId: NodeId?): NodeId? {
        return null
    }

    override fun cancelTimeoutOrTask() {
        electionTimeout.cancel()
    }

    override val state: RoleState
        get() {
            val state = DefaultRoleState(RoleName.CANDIDATE, term)
            state.votesCount = votesCount
            return state
        }


    override fun doStateEquals(role: AbstractNodeRole?): Boolean {
        val that = role as CandidateNodeRole
        return votesCount == that.votesCount
    }

    override fun toString(): String {
        return "CandidateNodeRole{" +
                "term=" + term +
                ", votesCount=" + votesCount +
                ", electionTimeout=" + electionTimeout +
                '}'
    }

}
