package raft.rpc.message

import raft.node.NodeId
import raft.rpc.Channel


abstract class AbstractRpcMessage<T> internal constructor(private val rpc: T, val sourceNodeId: NodeId?, val channel: Channel?) {

    fun get(): T {
        return rpc
    }

}
