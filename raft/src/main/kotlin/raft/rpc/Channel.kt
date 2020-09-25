package raft.rpc

import raft.rpc.message.AppendEntriesResult
import raft.rpc.message.AppendEntriesRpc
import raft.rpc.message.RequestVoteResult
import raft.rpc.message.RequestVoteRpc
import javax.annotation.Nonnull

interface Channel {
    /**
     * Write request vote rpc.
     *
     * @param rpc rpc
     */
    fun writeRequestVoteRpc(@Nonnull rpc: RequestVoteRpc?)

    /**
     * Write request vote result.
     *
     * @param result result
     */
    fun writeRequestVoteResult(@Nonnull result: RequestVoteResult?)

    /**
     * Write append entries rpc.
     *
     * @param rpc rpc
     */
    fun writeAppendEntriesRpc(@Nonnull rpc: AppendEntriesRpc?)

    /**
     * Write append entries result.
     *
     * @param result result
     */
    fun writeAppendEntriesResult(@Nonnull result: AppendEntriesResult?)

    /**
     * Close channel.
     */
    fun close()
}