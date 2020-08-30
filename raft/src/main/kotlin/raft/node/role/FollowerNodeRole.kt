
class FollowerNodeRole(name: RoleName, term: Int,
                       val votedFor: NodeId, val leaderId: NodeId
                       val electionTimeOut: ElectionTimeOut): AbstractNodeRole(name, term) {

    fun getVotedFor(): NodeId()
    fun getLeaderId(): NodeId()
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
