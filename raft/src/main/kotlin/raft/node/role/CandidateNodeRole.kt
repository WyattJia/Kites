class CandidateNodeRole(val votesCount: Int, val electionTimeOut: ElectionTimeOut, term: Int): AbstractNodeRole(name, term) {

    private val electionTimeOut: ElectionTimeOut

    constructor(term: Int, votesCount: Int, electionTimeOut: ElectionTimeOut) {
        super(RoleName.CANDIDATE, term)
        this.votesCount = votesCount
        this.electionTimeOut = electionTimeOut
    }

    override fun getLeaderId(selfId: NodeId?): NodeId {
        return null
    }

    public getVotedCount(): Int {
        return votesCount
    }

    public override fun cancelTimeoutOrTask() {
        electionTimeOut.cancel()
    }

    public override fun toString(): String {
        return "CandidateNodeRole{" +
                "term=" + term +
                ", votesCount=" + votesCount +
                ", electionTimeOut=" + electionTimeOut +
                "}"
    }
}
