package raft.service

import raft.node.NodeEndpoint

@Deprecated("Todo remove me")
class AddNodeCommand(nodeId: String?, host: String?, port: Int) {

    val nodeId: String? = null
    val host: String? = null
    val port = 0


    fun toNodeEndpoint(): NodeEndpoint? {
        return NodeEndpoint(nodeId!!, host!!, port)
    }
    }
