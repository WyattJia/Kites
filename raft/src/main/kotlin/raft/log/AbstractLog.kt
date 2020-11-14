package raft.log

import com.google.common.eventbus.EventBus
import org.slf4j.LoggerFactory
import raft.log.entry.Entry
import raft.log.entry.EntryMeta
import raft.log.entry.GeneralEntry
import raft.log.entry.NoOpEntry
import raft.log.sequence.EntrySequence
import raft.log.sequence.GroupConfigEntryList
import raft.log.snapshot.EntryInSnapshotException
import raft.log.snapshot.Snapshot
import raft.log.statemachine.StateMachineContext
import raft.node.NodeId
import raft.rpc.message.AppendEntriesRpc
import java.util.*


abstract class AbstractLog : Log {

    protected open val eventBus: EventBus = EventBus()
    protected var entrySequence: EntrySequence? = null
    protected var snapshot: Snapshot? = null
    protected  var  groupConfigEntryList: GroupConfigEntryList = GroupConfigEntryList()


    override var commitIndex = 0
        protected set

    override fun getLastEntryMeta(): EntryMeta {
        return if (entrySequence?.isEmpty!!) {
            EntryMeta(Entry.KIND_NO_OP, 0, 0)
        } else entrySequence!!.lastEntry?.meta!!
    }

    override open fun createAppendEntriesRpc(
        term: Int,
        selfId: NodeId,
        nextIndex: Int,
        maxEntries: Int
    ): AppendEntriesRpc {
        val nextLogIndex: Int = entrySequence!!.nextLogIndex
        require(nextIndex <= nextLogIndex) { "illegal next index $nextIndex" }
        if (nextIndex <= snapshot!!.lastIncludedIndex ) {
            throw EntryInSnapshotException(nextIndex)
        }
        val rpc = AppendEntriesRpc()
        rpc.messageid = (UUID.randomUUID().toString())
        rpc.term = (term)
        rpc.leaderId(selfId)
        rpc.leaderCommit(commitIndex)
        if (nextIndex == snapshot.getLastIncludedIndex() + 1) {
            rpc.setPrevLogIndex(snapshot.getLastIncludedIndex())
            rpc.setPrevLogTerm(snapshot.getLastIncludedTerm())
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
            rpc.setPrevLogIndex(entry.getIndex())
            rpc.setPrevLogTerm(entry.getTerm())
        }
        if (!entrySequence.isEmpty()) {
            val maxIndex =
                if (maxEntries == ALL_ENTRIES) nextLogIndex else Math.min(nextLogIndex, nextIndex + maxEntries)
            rpc.setEntries(entrySequence!!.subList(nextIndex, maxIndex))
        }
        return rpc
    }


    override fun isNewerThan(lastLogIndex: Int, lastLogTerm: Int): Boolean {
        val lastEntryMeta: EntryMeta = getLastEntryMeta()
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

    override fun appendEntry(term: Int, command: ByteArray): GeneralEntry {
        val entry = command.let { GeneralEntry(entrySequence!!.nextLogIndex, term, it) }
        entrySequence!!.append(entry)
        return entry
    }

    override fun appendEntriesFromLeader(prevLogIndex: Int, prevLogTerm: Int, entries: List<Entry>): Boolean {
        // check previous log
        if (!checkIfPreviousLogMatches(prevLogIndex, prevLogTerm)) {
            return false
        }
        // heartbeat
        if (entries.isEmpty()) {
            return true
        }
//        assert(prevLogIndex + 1 == leaderEntries[0]?.index ?: )
        val newEntries = removeUnmatchedLog(EntrySequenceView(entries))
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
            entrySequence?.append(leaderEntry)
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
        var followerEntryMeta: EntryMeta
        for (leaderEntry in leaderEntries) {
            logIndex = leaderEntry.index
            followerEntryMeta = entrySequence!!.getEntryMeta(logIndex)!!
            if (followerEntryMeta.term != leaderEntry.term) {
                return logIndex
            }
        }
        return leaderEntries.lastLogIndex + 1
    }

    private fun checkIfPreviousLogMatches(prevLogIndex: Int, prevLogTerm: Int): Boolean {
        val meta: EntryMeta? = entrySequence?.getEntryMeta(prevLogIndex)
        if (meta == null) {
            logger.debug("previous log $prevLogIndex not found")
            return false
        }
        val term: Int = meta.term
        if (term != prevLogTerm) {
            logger.debug("different term of previous log, local $term, remote $prevLogIndex")
            return false
        }
        return true
    }

    private fun removeEntriesAfter(index: Int) {
        if (entrySequence!!.isEmpty || index >= entrySequence!!.lastLogIndex) {
            return
        }
        logger.debug("remove entries after {}", index)
        entrySequence!!.removeAfter(index)
    }

    override fun advanceCommitIndex(newCommitIndex: Int, currentTerm: Int) {
        if (!validateNewCommitIndex(newCommitIndex, currentTerm)) {
            return
        }
        logger.debug("advance commit index from {} to {}", commitIndex, newCommitIndex)
        entrySequence!!.commit(newCommitIndex)
//        groupConfigsCommitted(newCommitIndex)
//        commitIndex = newCommitIndex
//        advanceApplyIndex()
    }

    // private fun advanceApplyIndex() {
    //     // start up and snapshot exists
    //     var lastApplied: Int = stateMachine.getLastApplied()
    //     val lastIncludedIndex: Int = snapshot.getLastIncludedIndex()
    //     if (lastApplied == 0 && lastIncludedIndex > 0) {
    //         assert(commitIndex >= lastIncludedIndex)
    //         applySnapshot(snapshot)
    //         lastApplied = lastIncludedIndex
    //     }
    //     for (entry in entrySequence.subList(lastApplied + 1, commitIndex + 1)) {
    //         applyEntry(entry)
    //     }
    // }

//    private fun applyEntry(entry: Entry) {
// skip no-op entry and membership-change entry
// if (isApplicable(entry)) {
//     stateMachine.applyLog(
//         stateMachineContext,
//         entry.index,
//         entry.commandBytes,
//         entrySequence.firstLogIndex
//     )
// }
//    }

    private fun isApplicable(entry: Entry): Boolean {
        return entry.kind == Entry.KIND_GENERAL
    }

    // private fun groupConfigsCommitted(newCommitIndex: Int) {
    //     for (groupConfigEntry in groupConfigEntryList.subList(commitIndex + 1, newCommitIndex + 1)) {
    //         eventBus.post(GroupConfigEntryCommittedEvent(groupConfigEntry))
    //     }
    // }

    private fun validateNewCommitIndex(newCommitIndex: Int, currentTerm: Int): Boolean {
        if (newCommitIndex <= commitIndex) {
            return false
        }
        val entry: Entry? = entrySequence?.getEntry(newCommitIndex)
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

    override fun close() {
        entrySequence?.close()
    }


    private class StateMachineContextImpl : StateMachineContext {
        override fun generateSnapshot(lastIncludedIndex: Int) {
            eventBus.post(SnapshotGenerateEvent(lastIncludedIndex))
        }
    }

    private class EntrySequenceView(private val entries: List<Entry>) :
        Iterable<Entry?> {
        var firstLogIndex = -1
        var lastLogIndex = -1

        operator fun get(index: Int): Entry? {
            return if (entries.isEmpty() || index < firstLogIndex || index > lastLogIndex) {
                null
            } else entries[index - firstLogIndex]
        }

        val isEmpty: Boolean
            get() = entries.isEmpty()

        fun subView(fromIndex: Int): EntrySequenceView {
            return if (entries.isEmpty() || fromIndex > lastLogIndex) {
                EntrySequenceView(emptyList<Entry>())
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

