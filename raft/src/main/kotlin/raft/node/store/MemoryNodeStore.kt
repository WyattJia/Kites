package raft.node.store

import raft.node.NodeId

class MemoryNodeStore:NodeStore {

    private var term: Int = 0
    private var votedFor: NodeId? = null

    init {
        this.term = term
        this.votedFor = votedFor
    }

    constructor(term: Int, votedFor: NodeId){
        this.term = term
        this.votedFor = votedFor
    }

    override fun getTerm(): Int {
        return term
    }

    override fun setTerm(term: Int) {
        this.term = term
    }

    override fun getVotedFor(): NodeId {
        return votedFor!!
    }

    override fun setVotedFor(votedFor: NodeId) {
        this.votedFor = votedFor
    }

    override fun close() {
        TODO("Not yet implemented")
    }


}