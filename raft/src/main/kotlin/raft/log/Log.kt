package raft.log

import raft.log.entry.*
import raft.log.statemachine.StateMachine
import raft.node.NodeEndpoint
import raft.node.NodeId
import raft.rpc.message.AppendEntriesRpc
import raft.rpc.message.InstallSnapshotRpc


/**
 * Log.
 *
 * @see raft.log.sequence.EntrySequence
 *
 * @see raft.log.snapshot.Snapshot
 */
interface Log {
    /**
     * Get meta of last entry.
     *
     * @return entry meta
     */
    val lastEntryMeta: EntryMeta?

    /**
     * Create append entries rpc from log.
     *
     * @param term       current term
     * @param selfId     self node id
     * @param nextIndex  next index
     * @param maxEntries max entries
     * @return append entries rpc
     */
    fun createAppendEntriesRpc(term: Int, selfId: NodeId?, nextIndex: Int, maxEntries: Int): AppendEntriesRpc?

    /**
     * Create install snapshot rpc from log.
     *
     * @param term   current term
     * @param selfId self node id
     * @param offset data offset
     * @param length data length
     * @return install snapshot rpc
     */
    fun createInstallSnapshotRpc(term: Int, selfId: NodeId?, offset: Int, length: Int): InstallSnapshotRpc?

    /**
     * Get last uncommitted group config entry.
     *
     * @return last committed group config entry, maybe `null`
     */
    val lastUncommittedGroupConfigEntry: GroupConfigEntry?

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
     * Append a NO-OP log entry.
     *
     * @param term current term
     * @return no-op entry
     */
    fun appendEntry(term: Int): NoOpEntry?

    /**
     * Append a general log entry.
     *
     * @param term    current term
     * @param command command in bytes
     * @return general entry
     */
    fun appendEntry(term: Int, command: ByteArray?): GeneralEntry?

    /**
     * Append a log entry for adding node.
     *
     * @param term            current term
     * @param nodeEndpoints   current node configs
     * @param newNodeEndpoint new node config
     * @return add node entry
     */
    fun appendEntryForAddNode(
        term: Int,
        nodeEndpoints: Set<NodeEndpoint?>?,
        newNodeEndpoint: NodeEndpoint?
    ): AddNodeEntry?

    /**
     * Append a log entry for removing node.
     *
     * @param term          current term
     * @param nodeEndpoints current node configs
     * @param nodeToRemove  node to remove
     * @return remove node entry
     */
    fun appendEntryForRemoveNode(term: Int, nodeEndpoints: Set<NodeEndpoint?>?, nodeToRemove: NodeId?): RemoveNodeEntry?

    /**
     * Append entries to log.
     *
     * @param prevLogIndex expected index of previous log entry
     * @param prevLogTerm  expected term of previous log entry
     * @param entries      entries to append
     * @return true if success, false if previous log check failed
     */
    fun appendEntriesFromLeader(prevLogIndex: Int, prevLogTerm: Int, entries: List<Entry?>?): Boolean

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
     * Install snapshot.
     *
     * @param rpc rpc
     * @return install snapshot state
     */
    fun installSnapshot(rpc: InstallSnapshotRpc?): InstallSnapshotState?

    /**
     * Generate snapshot.
     *
     * @param lastIncludedIndex last included index
     * @param groupConfig       group config
     */
    fun generateSnapshot(lastIncludedIndex: Int, groupConfig: Set<NodeEndpoint?>?)

    /**
     * Set state machine.
     *
     *
     * It will be called when
     *
     *  * apply the log entry
     *  * generate snapshot
     *  * apply snapshot
     *
     *
     * @param stateMachine state machine
     */
    fun setStateMachine(stateMachine: StateMachine?)

    /**
     * Close log files.
     */
    fun close()

    companion object {
        const val ALL_ENTRIES = -1
    }
}

