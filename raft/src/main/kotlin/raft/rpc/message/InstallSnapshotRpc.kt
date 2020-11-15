package raft.rpc.message

import raft.node.NodeEndpoint
import raft.node.NodeId


class InstallSnapshotRpc {
    var term = 0
    var leaderId: NodeId? = null
    var lastIndex = 0
    var lastTerm = 0
    var lastConfig: Set<NodeEndpoint>? = null
    var offset = 0
    var data: ByteArray? = null
    var isDone = false


    fun getDataLength(): Int {
        return data!!.size
    }

    override fun toString(): String {
        return "InstallSnapshotRpc{" +
                "data.size=" + (if (data != null) data!!.size else 0) +
                ", done=" + isDone +
                ", lastIndex=" + lastIndex +
                ", lastTerm=" + lastTerm +
                ", leaderId=" + leaderId +
                ", offset=" + offset +
                ", term=" + term +
                '}'
    }
}

