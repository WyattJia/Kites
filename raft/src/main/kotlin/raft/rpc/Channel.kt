package raft.rpc

import raft.rpc.message.*

/**
 * Channel between nodes.
 */
interface Channel {
    /**
     * Write request vote rpc.
     *
     * @param rpc rpc
     */
    fun writeRequestVoteRpc(rpc: RequestVoteRpc)

    /**
     * Write request vote result.
     *
     * @param result result
     */
    fun writeRequestVoteResult(result: RequestVoteResult)

    /**
     * Write append entries rpc.
     *
     * @param rpc rpc
     */
    fun writeAppendEntriesRpc(rpc: AppendEntriesRpc)

    /**
     * Write append entries result.
     *
     * @param result result
     */
    fun writeAppendEntriesResult(result: AppendEntriesResult)

    /**
     * Write install snapshot rpc.
     *
     * @param rpc rpc
     */
    fun writeInstallSnapshotRpc(rpc: InstallSnapshotRpc)

    /**
     * Write install snapshot result.
     *
     * @param result result
     */
    fun writeInstallSnapshotResult(result: InstallSnapshotResult)

    /**
     * Close channel.
     */
    fun close()
}