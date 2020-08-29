package raft.node

import raft.node.NodeId

abstract class AbstractNodeRole(val name: RoleName, val term: Int) {
//    val name: RoleName = name
//    val term: Int = term


//    fun getName():RoleName {
//       return name
//    }

    abstract fun cancelTimeoutOrTask()
//    fun getTerm(): Int {
//       return term
//    }

}


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

class CandidateNodeRole(val votesCount: Int, val electionTimeOut: ElectionTimeOut): AbstractNodeRole(name, term) {

    init {

    }

    constructor(term: Int, votesCount: Int, electionTimeOut: ElectionTimeOut) {
        super(RoleName.CANDIDATE, term)
        this.votesCount = votesCount
        this.electionTimeOut = electionTimeOut
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


class LeaderNodeRole(val logReplicationTask: LogReplicationTask):AbstractNodeRole(name, term) {
    init {
        super(RoleName.LEADER, term)
        this.logReplicationTask = logReplicationTask
    }

    public override fun cancelTimeoutOrTask() {
       logReplicationTask.cancel()
    }

    override fun toString(): String {
       return "LeaderNodeRole{term=" + term +
               ", logReplicationTasl=" + logReplicationTask +
               "}"
    }

}