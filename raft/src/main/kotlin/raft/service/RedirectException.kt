package raft.service

import raft.node.NodeId

class RedirectException(leaderId: NodeId) : ChannelException() {
    private val leaderId: NodeId
    fun getLeaderId(): NodeId {
        return leaderId
    }

    init {
        this.leaderId = leaderId
    }
}