package raft.node.role

import raft.node.AbstractNodeRole
import raft.node.NodeId
import raft.node.RoleName
import raft.schedule.ElectionTimeout
import javax.annotation.concurrent.Immutable


@Immutable
class FollowerNodeRole(term: Int, val votedFor: NodeId, private val leaderId: NodeId?, electionTimeout: ElectionTimeout) : AbstractNodeRole(RoleName.FOLLOWER, term) {
    private val electionTimeout: ElectionTimeout = electionTimeout

    override fun getLeaderId(selfId: NodeId?): NodeId? {
        return leaderId
    }

    override fun cancelTimeoutOrTask() {
        electionTimeout.cancel()
    }


    override val state: RoleState
        get() {
            val state = DefaultRoleState(RoleName.FOLLOWER, term)
            state.votedFor = votedFor
            state.leaderId = leaderId
            return state
        }


    override protected fun doStateEquals(role: AbstractNodeRole?): Boolean {

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

