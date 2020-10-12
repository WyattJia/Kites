package raft.node.store

import raft.node.NodeId

class MemoryNodeStore(private var term: Int, private var votedFor: NodeId?) : NodeStore {

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
    }


}