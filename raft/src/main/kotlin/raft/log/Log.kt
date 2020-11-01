package raft.log

import raft.log.entry.Entry
import raft.log.entry.EntryMeta
import raft.log.entry.GeneralEntry
import raft.log.entry.NoOpEntry
import raft.node.NodeEndpoint.NodeEndpoint
import raft.node.NodeId
import raft.rpc.message.AppendEntriesRpc

/**
 * Log.
 *
 * @see in.xnnyygn.xraft.core.log.sequence.EntrySequence
 *
 * @see in.xnnyygn.xraft.core.log.snapshot.Snapshot
 */
interface Log {

    val ALL_ENTRIES: Int
        get() = -1


    fun getLastEntryMeta(): EntryMeta


    /**
     * Create append entries rpc from log.
     *
     * @param term       current term
     * @param selfId     self node id
     * @param nextIndex  next index
     * @param maxEntries max entries
     * @return append entries rpc
     */
    fun createAppendEntriesRpc(term: Int, selfId: NodeId, nextIndex: Int, maxEntries: Int): AppendEntriesRpc


    /**
     * Get next log index.
     *
     * @return next log index
     */
    val nextIndex: Int

    /**
     * Get commit index.
     *
     * @return commit index
     */
    val commitIndex: Int

    /**
     * Test if last log self is new than last log of leader.
     *
     * @param lastLogIndex last log index
     * @param lastLogTerm  last log term
     * @return true if last log self is newer than last log of leader, otherwise false
     */
    fun isNewerThan(lastLogIndex: Int, lastLogTerm: Int): Boolean


    /**
     * Append entries to log.
     *
     * @param prevLogIndex expected index of previous log entry
     * @param prevLogTerm  expected term of previous log entry
     * @param entries      entries to append
     * @return true if success, false if previous log check failed
     */
    fun appendEntriesFromLeader(prevLogIndex: Int, prevLogTerm: Int, entries: List<Entry>): Boolean

    /**
     * Advance commit index.
     *
     *
     *
     * The log entry with new commit index must be the same term as the one in parameter,
     * otherwise commit index will not change.
     *
     *
     * @param newCommitIndex new commit index
     * @param currentTerm    current term
     */
    fun advanceCommitIndex(newCommitIndex: Int, currentTerm: Int)

    /**
     * Append a NO-OP log entry.
     *
     * @param term current term
     * @return no-op entry
     */
    fun appendEntry(term: Int): NoOpEntry

    /**
     * Append a general log entry.
     *
     * @param term    current term
     * @param command command in bytes
     * @return general entry
     */
    fun appendEntry(term: Int, command: ByteArray): GeneralEntry

    /**
     * Generate snapshot.
     *
     * @param lastIncludedIndex last included index
     * @param groupConfig       group config
     */
    fun generateSnapshot(lastIncludedIndex: Int, groupConfig: Set<NodeEndpoint?>?)

    /**
     * Close log files.
     */
    fun close()

    companion object {
        const val ALL_ENTRIES = -1
    }
}
