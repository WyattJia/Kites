package raft.rpc

import raft.node.NodeEndpoint.NodeEndpoint
import raft.rpc.message.*

interface Connector {
    fun initialize()

    // Send requests to multi endpoints.
    fun sendRequestVote(
        rpc: RequestVoteRpc,
        destinationEndpoints: Collection<NodeEndpoint>
    )

    fun replyRequestVote(
        result: RequestVoteResult,
        rpcMessage: RequestVoteRpcMessage
    )

    fun sendAppendEntries(
        rpc: AppendEntriesRpc,
        destinationEndpoint: NodeEndpoint
    )

    fun replyAppendEntries(
        result: AppendEntriesResult,
        destinationEndpoint: AppendEntriesRpcMessage
    )

    fun close()


    /**
     * Called when node becomes leader.
     *
     *
     * Connector may use this chance to close inbound channels.
     *
     */
    fun resetChannels()

}