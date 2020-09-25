package raft.rpc.message

import raft.node.NodeId
import raft.rpc.Channel


abstract class AbstractRpcMessage<T> internal constructor(private val rpc: T, private val sourceNodeId: NodeId,private val channel: Channel) {
    fun get(): T {
        return rpc
    }

    fun getSourceNodeId(): NodeId {
        return sourceNodeId
    }

    fun getChannel(): Channel {
        return channel
    }

}
