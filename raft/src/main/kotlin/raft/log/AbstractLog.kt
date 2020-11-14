package raft.log

import com.google.common.eventbus.EventBus
import org.slf4j.LoggerFactory
import raft.log.Log.Companion.ALL_ENTRIES
import raft.log.entry.*
import raft.log.event.GroupConfigEntryBatchRemovedEvent
import raft.log.event.GroupConfigEntryCommittedEvent
import raft.log.event.GroupConfigEntryFromLeaderAppendEvent
import raft.log.event.SnapshotGenerateEvent
import raft.log.sequence.EntrySequence
import raft.log.sequence.GroupConfigEntryList
import raft.log.snapshot.*
import raft.log.statemachine.EmptyStateMachine
import raft.log.statemachine.StateMachine
import raft.log.statemachine.StateMachineContext
import raft.node.NodeEndpoint
import raft.node.NodeId
import raft.rpc.message.AppendEntriesRpc
import raft.rpc.message.InstallSnapshotRpc
import java.io.IOException
import java.util.*


abstract class AbstractLog(protected val eventBus: EventBus) : Log {
    protected var snapshot: Snapshot? = null
    protected var entrySequence: EntrySequence? = null
    protected var snapshotBuilder: SnapshotBuilder<Snapshot?> = NullSnapshotBuilder()
    protected var groupConfigEntryList = GroupConfigEntryList()
    private val stateMachineContext: StateMachineContext = StateMachineContextImpl()
    private var stateMachine: StateMachine = EmptyStateMachine()
    override fun setStateMachine(stateMachine: StateMachine?) {
        this.stateMachine = stateMachine!!
    }

    override var commitIndex = 0
        protected set

    override val lastEntryMeta: EntryMeta
        get() = if (entrySequence!!.isEmpty) {
            EntryMeta(Entry.KIND_NO_OP, snapshot!!.lastIncludedIndex, snapshot!!.lastIncludedTerm)
        } else entrySequence!!.lastEntry?.meta!!

    override fun createAppendEntriesRpc(term: Int, selfId: NodeId?, nextIndex: Int, maxEntries: Int): AppendEntriesRpc {
        val nextLogIndex: Int = entrySequence!!.nextLogIndex
        require(nextIndex <= nextLogIndex) { "illegal next index $nextIndex" }
        if (nextIndex <= snapshot!!.lastIncludedIndex) {
            throw EntryInSnapshotException(nextIndex)
        }
        val rpc = AppendEntriesRpc()
        rpc.messageId = UUID.randomUUID().toString()
        rpc.term = term
        rpc.leaderId = selfId
        rpc.leaderCommit = commitIndex
        if (nextIndex == snapshot!!.lastIncludedIndex + 1) {
            rpc.prevLogIndex = snapshot!!.lastIncludedIndex
            rpc.prevLogTerm = snapshot!!.lastIncludedTerm
        } else {
            // if entry sequence is empty,
            // snapshot.lastIncludedIndex + 1 == nextLogIndex,
            // so it has been rejected at the first line.
            //
            // if entry sequence is not empty,
            // snapshot.lastIncludedIndex + 1 < nextIndex <= nextLogIndex
            // and snapshot.lastIncludedIndex + 1 = firstLogIndex
            //     nextLogIndex = lastLogIndex + 1
            // then firstLogIndex < nextIndex <= lastLogIndex + 1
            //      firstLogIndex + 1 <= nextIndex <= lastLogIndex + 1
            //      firstLogIndex <= nextIndex - 1 <= lastLogIndex
            // it is ok to get entry without null check
            val entry = entrySequence!!.getEntry(nextIndex - 1)!!
            rpc.prevLogIndex = entry.index
            rpc.prevLogTerm = entry.term
        }
        if (!entrySequence!!.isEmpty) {
            val maxIndex =
                if (maxEntries == ALL_ENTRIES) nextLogIndex else Math.min(nextLogIndex, nextIndex + maxEntries)
            rpc.entries =entrySequence!!.subList(nextIndex, maxIndex)
        }
        return rpc
    }

    override fun createInstallSnapshotRpc(term: Int, selfId: NodeId?, offset: Int, length: Int): InstallSnapshotRpc {
        val rpc = InstallSnapshotRpc()
        rpc.term = term
        rpc.leaderId = selfId
        rpc.lastIndex = snapshot!!.lastIncludedIndex
        rpc.lastTerm = snapshot!!.lastIncludedTerm
        if (offset == 0) {
            rpc.lastConfig = snapshot!!.lastConfig as Set<NodeEndpoint>?
        }
        rpc.offset = offset
        val chunk: SnapshotChunk = snapshot!!.readData(offset, length)!!
        rpc.data = chunk.toByteArray()
        rpc.isDone = chunk.isLastChunk
        return rpc
    }

    override val lastUncommittedGroupConfigEntry: GroupConfigEntry?
        get() {
            val lastEntry: GroupConfigEntry = groupConfigEntryList.last!!
            return if (lastEntry.index > commitIndex) lastEntry else null
        }
    override val nextIndex: Int
        get() = entrySequence!!.nextLogIndex

    override fun isNewerThan(lastLogIndex: Int, lastLogTerm: Int): Boolean {
        val lastEntryMeta = lastEntryMeta
        logger.debug(
            "last entry ({}, {}), candidate ({}, {})",
            lastEntryMeta.index,
            lastEntryMeta.term,
            lastLogIndex,
            lastLogTerm
        )
        return lastEntryMeta.term > lastLogTerm || lastEntryMeta.index > lastLogIndex
    }

    override fun appendEntry(term: Int): NoOpEntry {
        val entry = NoOpEntry(entrySequence!!.nextLogIndex, term)
        entrySequence!!.append(entry)
        return entry
    }

    override fun appendEntry(term: Int, command: ByteArray?): GeneralEntry {
        val entry = GeneralEntry(entrySequence!!.nextLogIndex, term, command!!)
        entrySequence!!.append(entry)
        return entry
    }

    override fun appendEntryForAddNode(
        term: Int,
        nodeEndpoints: Set<NodeEndpoint?>?,
        newNodeEndpoint: NodeEndpoint?
    ): AddNodeEntry? {
        val entry = newNodeEndpoint?.let { AddNodeEntry(entrySequence!!.nextLogIndex, term, nodeEndpoints, it) }
        entrySequence!!.append(entry)
        if (entry != null) {
            groupConfigEntryList.add(entry)
        }
        return entry
    }

    override fun appendEntryForRemoveNode(
        term: Int,
        nodeEndpoints: Set<NodeEndpoint?>?,
        nodeToRemove: NodeId?
    ): RemoveNodeEntry? {
        val entry = nodeToRemove?.let { RemoveNodeEntry(entrySequence!!.nextLogIndex, term, nodeEndpoints, it) }
        entrySequence?.append(entry)
        if (entry != null) {
            groupConfigEntryList.add(entry)
        }
        return entry
    }

    override fun appendEntriesFromLeader(prevLogIndex: Int, prevLogTerm: Int, entries: List<Entry?>?): Boolean {
        // check previous log
        if (!checkIfPreviousLogMatches(prevLogIndex, prevLogTerm)) {
            return false
        }
        // heartbeat
        if (entries != null) {
            if (entries.isEmpty()) {
                return true
            }
        }
        assert(prevLogIndex + 1 == entries?.get(0)!!.index)
        val newEntries = removeUnmatchedLog(EntrySequenceView(entries as List<Entry>))
        appendEntriesFromLeader(newEntries)
        return true
    }

    private fun appendEntriesFromLeader(leaderEntries: EntrySequenceView) {
        if (leaderEntries.isEmpty) {
            return
        }
        logger.debug(
            "append entries from leader from {} to {}",
            leaderEntries.firstLogIndex,
            leaderEntries.lastLogIndex
        )
        for (leaderEntry in leaderEntries) {
            appendEntryFromLeader(leaderEntry)
        }
    }

    private fun appendEntryFromLeader(leaderEntry: Entry) {
        entrySequence!!.append(leaderEntry)
        if (leaderEntry is GroupConfigEntry) {
            eventBus.post(
                GroupConfigEntryFromLeaderAppendEvent(
                    leaderEntry as GroupConfigEntry
                )
            )
        }
    }

    private fun removeUnmatchedLog(leaderEntries: EntrySequenceView): EntrySequenceView {
        assert(!leaderEntries.isEmpty)
        val firstUnmatched = findFirstUnmatchedLog(leaderEntries)
        removeEntriesAfter(firstUnmatched - 1)
        return leaderEntries.subView(firstUnmatched)
    }

    private fun findFirstUnmatchedLog(leaderEntries: EntrySequenceView): Int {
        assert(!leaderEntries.isEmpty)
        var logIndex: Int
        var followerEntryMeta: EntryMeta?
        for (leaderEntry in leaderEntries) {
            logIndex = leaderEntry.index
            followerEntryMeta = entrySequence!!.getEntryMeta(logIndex)
            if (followerEntryMeta == null || followerEntryMeta.term != leaderEntry.term) {
                return logIndex
            }
        }
        return leaderEntries.lastLogIndex + 1
    }

    private fun checkIfPreviousLogMatches(prevLogIndex: Int, prevLogTerm: Int): Boolean {
        val lastIncludedIndex: Int = snapshot!!.lastIncludedIndex
        if (prevLogIndex < lastIncludedIndex) {
            logger.debug("previous log index {} < snapshot's last included index {}", prevLogIndex, lastIncludedIndex)
            return false
        }
        if (prevLogIndex == lastIncludedIndex) {
            val lastIncludedTerm: Int = snapshot!!.lastIncludedTerm
            if (prevLogTerm != lastIncludedTerm) {
                logger.debug(
                    "previous log index matches snapshot's last included index, " +
                            "but term not (expected {}, actual {})", lastIncludedTerm, prevLogTerm
                )
                return false
            }
            return true
        }
        val entry = entrySequence!!.getEntry(prevLogIndex)
        if (entry == null) {
            logger.debug("previous log {} not found", prevLogIndex)
            return false
        }
        val term: Int = entry.term
        if (term != prevLogTerm) {
            logger.debug("different term of previous log, local {}, remote {}", term, prevLogTerm)
            return false
        }
        return true
    }

    private fun removeEntriesAfter(index: Int) {
        if (entrySequence!!.isEmpty || index >= entrySequence!!.lastLogIndex) {
            return
        }
        val lastApplied: Int = stateMachine.lastApplied
        if (index < lastApplied && entrySequence!!.subList(index + 1, lastApplied + 1).stream()
                .anyMatch { entry: Entry ->
                    isApplicable(
                        entry
                    )
                }
        ) {
            logger.warn("applied log removed, reapply from start")
            applySnapshot(snapshot)
            logger.debug("apply log from {} to {}", entrySequence!!.firstLogIndex, index)
            entrySequence!!.subList(entrySequence!!.firstLogIndex, index + 1).forEach { entry: Entry ->
                applyEntry(
                    entry
                )
            }
        }
        logger.debug("remove entries after {}", index)
        entrySequence!!.removeAfter(index)
        if (index < commitIndex) {
            commitIndex = index
        }
        val firstRemovedEntry: GroupConfigEntry? = groupConfigEntryList.removeAfter(index)
        if (firstRemovedEntry != null) {
            logger.info("group config removed")
            eventBus.post(GroupConfigEntryBatchRemovedEvent(firstRemovedEntry))
        }
    }

    override fun advanceCommitIndex(newCommitIndex: Int, currentTerm: Int) {
        if (!validateNewCommitIndex(newCommitIndex, currentTerm)) {
            return
        }
        logger.debug("advance commit index from {} to {}", commitIndex, newCommitIndex)
        entrySequence!!.commit(newCommitIndex)
        groupConfigsCommitted(newCommitIndex)
        commitIndex = newCommitIndex
        advanceApplyIndex()
    }

    override fun generateSnapshot(lastIncludedIndex: Int, groupConfig: Set<NodeEndpoint?>?) {
        logger.info("generate snapshot, last included index {}", lastIncludedIndex)
        val lastAppliedEntryMeta = entrySequence!!.getEntryMeta(lastIncludedIndex)
        replaceSnapshot(generateSnapshot(lastAppliedEntryMeta, groupConfig))
    }

    private fun advanceApplyIndex() {
        // start up and snapshot exists
        var lastApplied: Int = stateMachine.lastApplied
        val lastIncludedIndex: Int = snapshot!!.lastIncludedIndex
        if (lastApplied == 0 && lastIncludedIndex > 0) {
            assert(commitIndex >= lastIncludedIndex)
            applySnapshot(snapshot)
            lastApplied = lastIncludedIndex
        }
        for (entry in entrySequence!!.subList(lastApplied + 1, commitIndex + 1)) {
            applyEntry(entry)
        }
    }

    private fun applySnapshot(snapshot: Snapshot?) {
        logger.debug("apply snapshot, last included index {}", snapshot!!.lastIncludedIndex)
        try {
            stateMachine.applySnapshot(snapshot)
        } catch (e: IOException) {
            throw LogException("failed to apply snapshot", e)
        }
    }

    private fun applyEntry(entry: Entry) {
        // skip no-op entry and membership-change entry
        if (isApplicable(entry)) {
            stateMachine.applyLog(
                stateMachineContext,
                entry.index,
                entry.commandBytes!!,
                entrySequence!!.firstLogIndex
            )
        }
    }

    private fun isApplicable(entry: Entry): Boolean {
        return entry.kind == Entry.KIND_GENERAL
    }

    private fun groupConfigsCommitted(newCommitIndex: Int) {
        for (groupConfigEntry in groupConfigEntryList.subList(commitIndex + 1, newCommitIndex + 1)!!) {
            eventBus.post(GroupConfigEntryCommittedEvent(groupConfigEntry as GroupConfigEntry))
        }
    }

    private fun validateNewCommitIndex(newCommitIndex: Int, currentTerm: Int): Boolean {
        if (newCommitIndex <= commitIndex) {
            return false
        }
        val entry = entrySequence!!.getEntry(newCommitIndex)
        if (entry == null) {
            logger.debug("log of new commit index {} not found", newCommitIndex)
            return false
        }
        if (entry.term != currentTerm) {
            logger.debug("log term of new commit index != current term ({} != {})", entry.term, currentTerm)
            return false
        }
        return true
    }

    protected abstract fun generateSnapshot(
        lastAppliedEntryMeta: EntryMeta?,
        groupConfig: Set<NodeEndpoint?>?
    ): Snapshot?

    override fun installSnapshot(rpc: InstallSnapshotRpc?): InstallSnapshotState? {
        if (rpc!!.lastIndex <= snapshot!!.lastIncludedIndex) {
            logger.debug(
                "snapshot's last included index from rpc <= current one ({} <= {}), ignore",
                rpc.lastIndex, snapshot!!.lastIncludedIndex
            )
            return InstallSnapshotState(InstallSnapshotState.StateName.ILLEGAL_INSTALL_SNAPSHOT_RPC)
        }
        if (rpc.offset == 0) {
            assert(rpc.lastConfig != null)
            snapshotBuilder.close()
            snapshotBuilder = newSnapshotBuilder(rpc)
        } else {
            snapshotBuilder.append(rpc)
        }
        if (!rpc.isDone) {
            return InstallSnapshotState(InstallSnapshotState.StateName.INSTALLING)
        }
        val newSnapshot: Snapshot = snapshotBuilder.build()!!
        applySnapshot(newSnapshot)
        replaceSnapshot(newSnapshot)
        val lastIncludedIndex: Int = snapshot!!.lastIncludedIndex
        if (commitIndex < lastIncludedIndex) {
            commitIndex = lastIncludedIndex
        }
        return InstallSnapshotState(
            InstallSnapshotState.StateName.INSTALLED,
            newSnapshot.lastConfig as Set<NodeEndpoint>?
        )
    }

    protected abstract fun newSnapshotBuilder(firstRpc: InstallSnapshotRpc?): SnapshotBuilder<Snapshot?>
    protected abstract fun replaceSnapshot(newSnapshot: Snapshot?)

    override fun close() {
        snapshot!!.close()
        entrySequence!!.close()
        snapshotBuilder.close()
        stateMachine.shutdown()
    }

    private inner class StateMachineContextImpl : StateMachineContext {
        override fun generateSnapshot(lastIncludedIndex: Int) {
            eventBus.post(SnapshotGenerateEvent(lastIncludedIndex))
        }
    }

    private class EntrySequenceView internal constructor(private val entries: List<Entry>) :
        Iterable<Entry?> {
        var firstLogIndex = 0
        var lastLogIndex = 0

        operator fun get(index: Int): Entry? {
            return if (entries.isEmpty() || index < firstLogIndex || index > lastLogIndex) {
                null
            } else entries[index - firstLogIndex]
        }

        val isEmpty: Boolean
            get() = entries.isEmpty()

        fun subView(fromIndex: Int): EntrySequenceView {
            return if (entries.isEmpty() || fromIndex > lastLogIndex) {
                EntrySequenceView(emptyList())
            } else EntrySequenceView(
                entries.subList(fromIndex - firstLogIndex, entries.size)
            )
        }

        override fun iterator(): Iterator<Entry> {
            return entries.iterator()
        }

        init {
            if (!entries.isEmpty()) {
                firstLogIndex = entries[0].index
                lastLogIndex = entries[entries.size - 1].index
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractLog::class.java)
    }
}


