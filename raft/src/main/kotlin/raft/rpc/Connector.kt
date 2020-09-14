package raft.rpc

import raft.node.NodeEndpoint.NodeEndpoint
import raft.rpc.message.AppendEntriesResult
import raft.rpc.message.AppendEntriesRpc
import raft.rpc.message.RequestVoteResult
import raft.rpc.message.RequestVoteRpc

interface Connector {
    fun initialize()
    fun sendRequestVote(
        rpc: RequestVoteRpc,
        destinationEndpoints: Collection<NodeEndpoint>
    )

    fun replyRequestVote(
        result: RequestVoteResult,
        destinationEndpoint: NodeEndpoint
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