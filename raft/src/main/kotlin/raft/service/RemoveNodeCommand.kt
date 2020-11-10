package raft.service

import raft.node.NodeId


class RemoveNodeCommand(nodeId: String?) {
    private val nodeId: NodeId = NodeId(nodeId!!)
    fun getNodeId(): NodeId {
        return nodeId
    }

}

