package raft.node.store

import raft.node.NodeId.NodeId

interface NodeStore {
    fun getTerm():Int
    fun setTerm(term: Int)
    fun getVotedFor():NodeId
    fun setVotedFor(votedFor:NodeId)
    fun close()
}