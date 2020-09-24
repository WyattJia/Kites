package raft.rpc

import raft.node.NodeEndpoint.NodeEndpoint
import raft.rpc.message.*

interface Connector {
    fun initialize()
    fun sendRequestVote(
        rpc: RequestVoteRpc,
        destinationEndpoints: Collection<NodeEndpoint>
    )

    fun replyRequestVote(
        result: RequestVoteResult,
        destinationEndpoint: RequestVoteRpcMessage?
    )

    fun sendAppendEntries(
        rpc: AppendEntriesRpc,
        destinationEndpoint: NodeEndpoint
    )

    fun replyAppendEntries(
        result: AppendEntriesResult,
        destinationEndpoint: NodeEndpoint
    )

    fun close()

}