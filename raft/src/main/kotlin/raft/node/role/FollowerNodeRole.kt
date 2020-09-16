import javax.annotation.concurrent.Immutable

import raft.node.AbstractNodeRole
import raft.node.NodeId.NodeId
import raft.node.RoleName
import raft.node.role.RoleState
import raft.schedule.ElectionTimeout


@Immutable
class FollowerNodeRole(term: Int, private val votedFor: NodeId, private val leaderId: NodeId, electionTimeout: ElectionTimeout) : AbstractNodeRole(RoleName.FOLLOWER, term) {
    private val electionTimeout: ElectionTimeout = electionTimeout

    override fun getLeaderId(selfId: NodeId?): NodeId {
        return leaderId
    }

    override fun cancelTimeoutOrTask() {
        electionTimeout.cancel()
    }


    override val state: RoleState
        get() {
            val state = DefaultRoleState(RoleName.FOLLOWER, term)
            state.setVotedFor(votedFor)
            state.setLeaderId(leaderId)
            return state
        }

    override fun doStateEquals(role: AbstractNodeRole?): Boolean {
        TODO("Not yet implemented")
    }

    override fun doStateEquals(role: AbstractNodeRole): Boolean {
        val that = role as FollowerNodeRole
        return votedFor == that.votedFor && leaderId == that.leaderId
    }

    override fun toString(): String {
        return "FollowerNodeRole{" +
                "term=" + term +
                ", leaderId=" + leaderId +
                ", votedFor=" + votedFor +
                ", electionTimeout=" + electionTimeout +
                '}'
    }

}

