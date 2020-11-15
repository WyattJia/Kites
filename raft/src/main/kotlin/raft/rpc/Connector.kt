package raft.rpc

import raft.node.NodeEndpoint
import raft.rpc.message.*


/**
 * Connector.
 */
interface Connector {
    /**
     * Initialize connector.
     *
     *
     * SHOULD NOT call more than one.
     *
     */
    fun initialize()

    /**
     * Send request vote rpc.
     *
     *
     * Remember to exclude self node before sending.
     *
     *
     *
     * Do nothing if destination endpoints is empty.
     *
     *
     * @param rpc                  rpc
     * @param destinationEndpoints destination endpoints
     */
    fun sendRequestVote(rpc: RequestVoteRpc, destinationEndpoints: Collection<NodeEndpoint>)

    /**
     * Reply request vote result.
     *
     * @param result     result
     * @param rpcMessage rpc message
     */
    fun replyRequestVote(result: RequestVoteResult, rpcMessage: RequestVoteRpcMessage)

    /**
     * Send append entries rpc.
     *
     * @param rpc                 rpc
     * @param destinationEndpoint destination endpoint
     */
    fun sendAppendEntries(rpc: AppendEntriesRpc, destinationEndpoint: NodeEndpoint)

    /**
     * Reply append entries result.
     *
     * @param result result
     * @param rpcMessage rpc message
     */
    fun replyAppendEntries(result: AppendEntriesResult, rpcMessage: AppendEntriesRpcMessage)

    /**
     * Send install snapshot rpc.
     *
     * @param rpc rpc
     * @param destinationEndpoint destination endpoint
     */
    fun sendInstallSnapshot(rpc: InstallSnapshotRpc, destinationEndpoint: NodeEndpoint)

    /**
     * Reply install snapshot result.
     *
     * @param result result
     * @param rpcMessage rpc message
     */
    fun replyInstallSnapshot(result: InstallSnapshotResult, rpcMessage: InstallSnapshotRpcMessage)

    /**
     * Called when node becomes leader.
     *
     *
     * Connector may use this chance to close inbound channels.
     *
     */
    fun resetChannels()

    /**
     * Close connector.
     */
    fun close()
}

