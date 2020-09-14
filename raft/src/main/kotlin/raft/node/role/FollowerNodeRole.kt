import raft.node.AbstractNodeRole
import raft.node.NodeId.NodeId
import raft.node.RoleName

class FollowerNodeRole(name: RoleName, term: Int,
                       val votedFor: NodeId, val leaderId: NodeId
                       val electionTimeOut: ElectionTimeOut): AbstractNodeRole(name, term) {

    @JvmName("getVotedFor1")
    fun getVotedFor(): NodeId {
    }

    @JvmName("getLeaderId1")
    fun getLeaderId(): NodeId {
    }

    fun cancelTimeOutOrTask() {
        electionTimeOut.cancel()
    }

    public override fun toString(): String {
        return "FollwerNodeRole{" +
                "term=" + term +
                ", LeaderId=" + leaderId +
                ", votedFor=" + votedFor +
                "electionTimeout=" + electionTimeOut +
                "}"
    }
}
