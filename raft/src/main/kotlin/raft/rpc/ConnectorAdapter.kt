package raft.rpc

import raft.node.NodeEndpoint
import raft.rpc.message.*


abstract class ConnectorAdapter : Connector {
    override fun initialize() {}
    override fun sendRequestVote(rpc: RequestVoteRpc, destinationEndpoints: Collection<NodeEndpoint>) {}
    override fun replyRequestVote(result: RequestVoteResult, rpcMessage: RequestVoteRpcMessage) {}
    override fun sendAppendEntries(rpc: AppendEntriesRpc, destinationEndpoint: NodeEndpoint) {}
    override fun replyAppendEntries(result: AppendEntriesResult, rpcMessage: AppendEntriesRpcMessage) {}
    override fun sendInstallSnapshot(rpc: InstallSnapshotRpc, destinationEndpoint: NodeEndpoint) {}
    override fun replyInstallSnapshot(
        result: InstallSnapshotResult,
        rpcMessage: InstallSnapshotRpcMessage
    ) {
    }

    override fun resetChannels() {}
    override fun close() {}
}
