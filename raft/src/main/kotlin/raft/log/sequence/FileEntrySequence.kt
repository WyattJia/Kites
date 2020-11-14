package raft.log.sequence

import raft.log.LogDir
import raft.log.LogException
import raft.log.entry.Entry
import raft.log.entry.EntryFactory
import raft.log.entry.EntryMeta
import raft.log.entry.GroupConfigEntry
import java.io.IOException
import java.util.*


class FileEntrySequence(override var nextLogIndex: LogDir, logIndexOffset: Int) : AbstractEntrySequence() {
    private val entryFactory: EntryFactory = EntryFactory()
    private lateinit var entriesFile: EntriesFile
    private lateinit var entryIndexFile: EntryIndexFile
    private val pendingEntries: LinkedList<Entry> = LinkedList<Entry>()
    override var commitIndex = 0
        private set
//
//    override val lastLogIndex: Int
//        get() {
//            if (isEmpty) {
//                throw EmptySequenceException()
//            }
//            return doGetLastLogIndex()
//        }


//    constructor(logDir: LogDir) {
//        try {
//            entriesFile = EntriesFile(logDir.entriesFile)
//            entryIndexFile = EntryIndexFile(logDir.entryOffsetIndexFile)
//            initialize()
//        } catch (e: IOException) {
//            throw LogException("failed to open entries file or entry index file", e)
//        }
//    }
//
//    constructor(entriesFile: EntriesFile, entryIndexFile: EntryIndexFile) {
//        this.entriesFile = entriesFile
//        this.entryIndexFile = entryIndexFile
//        initialize()
//    }

    private fun initialize() {
        if (entryIndexFile.isEmpty) {
            commitIndex = logIndexOffset - 1
            return
        }
        logIndexOffset = entryIndexFile.getMinEntryIndex()
        nextLogIndex = entryIndexFile.getMaxEntryIndex() + 1
        commitIndex = entryIndexFile.getMaxEntryIndex()
    }


    override fun doSubList(fromIndex: Int, toIndex: Int): List<Entry> {
        val result: MutableList<Entry> = ArrayList<Entry>()

        // entries from file
        if (!entryIndexFile.isEmpty && fromIndex <= entryIndexFile.getMaxEntryIndex()) {
            val maxIndex = Math.min(entryIndexFile.getMaxEntryIndex() + 1, toIndex)
            for (i in fromIndex until maxIndex) {
                result.add(getEntryInFile(i))
            }
        }

        // entries from pending entries
        if (!pendingEntries.isEmpty() && toIndex > pendingEntries.getFirst().index) {
            val iterator: Iterator<Entry> = pendingEntries.iterator()
            var entry: Entry
            var index: Int
            while (iterator.hasNext()) {
                entry = iterator.next()
                index = entry.index
                if (index >= toIndex) {
                    break
                }
                if (index >= fromIndex) {
                    result.add(entry)
                }
            }
        }
        return result
    }

    override fun append(entry: Entry?) {
        TODO("Not yet implemented")
    }

    override fun append(entries: List<Entry?>?) {
        TODO("Not yet implemented")
    }


    override fun doGetEntry(index: Int): Entry {
        if (!pendingEntries.isEmpty()) {
            val firstPendingEntryIndex: Int = pendingEntries.getFirst().index
            if (index >= firstPendingEntryIndex) {
                return pendingEntries[index - firstPendingEntryIndex]
            }
        }
        assert(!entryIndexFile.isEmpty)
        return getEntryInFile(index)
    }

    override fun getEntryMeta(index: Int): EntryMeta? {
        if (!isEntryPresent(index)) {
            return null
        }
        return if (entryIndexFile.isEmpty) {
            pendingEntries[index - doGetFirstLogIndex()].meta
        } else entryIndexFile[index]!!.toEntryMeta()
    }

    private fun getEntryInFile(index: Int): Entry {
        val offset = entryIndexFile.getOffset(index)!!
        return try {
            entriesFile.loadEntry(offset, entryFactory)
        } catch (e: IOException) {
            throw LogException("failed to load entry $index", e)
        }
    }

    override val lastEntry: Entry?
        get() {
            if (isEmpty) {
                return null
            }
            if (!pendingEntries.isEmpty()) {
                return pendingEntries.getLast()
            }
            assert(!entryIndexFile.isEmpty)
            return getEntryInFile(entryIndexFile.getMaxEntryIndex())
        }

    protected override fun doAppend(entry: Entry) {
        pendingEntries.add(entry)
    }

    override fun commit(index: Int) {
        require(index >= commitIndex) { "commit index < $commitIndex" }
        if (index == commitIndex) {
            return
        }
        require(
            !(pendingEntries.isEmpty() || pendingEntries.getLast().index < index)
        ) { "no entry to commit or commit index exceed" }
        var offset: Long
        var entry: Entry? = null
        try {
            for (i in commitIndex + 1..index) {
                entry = pendingEntries.removeFirst()
                offset = entriesFile.appendEntry(entry)
                entryIndexFile.appendEntryIndex(i, offset, entry.kind, entry.term)
                commitIndex = i
            }
        } catch (e: IOException) {
            throw LogException("failed to commit entry $entry", e)
        }
    }

    override fun doRemoveAfter(index: Int) {
        if (!pendingEntries.isEmpty() && index >= pendingEntries.first.index - 1) {
            // remove last n entries in pending entries
            for (i in index + 1..doGetLastLogIndex()) {
                pendingEntries.removeLast()
            }
            nextLogIndex = index + 1
            return
        }
        try {
            if (index >= doGetFirstLogIndex()) {
                pendingEntries.clear()
                // remove entries whose index >= (index + 1)
                entriesFile.truncate(entryIndexFile.getOffset(index + 1)!!)
                entryIndexFile.removeAfter(index)
                nextLogIndex = index + 1
                commitIndex = index
            } else {
                pendingEntries.clear()
                entriesFile.clear()
                entryIndexFile.clear()
                nextLogIndex = logIndexOffset
                commitIndex = logIndexOffset - 1
            }
        } catch (e: IOException) {
            throw LogException(e)
        }
    }


    override fun buildGroupConfigEntryList(): GroupConfigEntryList? {
        val list = GroupConfigEntryList()

        // check file
        try {
            var entryKind: Int
            for (indexItem in entryIndexFile) {
                entryKind = indexItem!!.kind
                if (entryKind == Entry.KIND_ADD_NODE || entryKind == Entry.KIND_REMOVE_NODE) {
                    list.add(entriesFile.loadEntry(indexItem.offset, entryFactory) as GroupConfigEntry)
                }
            }
        } catch (e: IOException) {
            throw LogException("failed to load entry", e)
        }

        // check pending entries
        for (entry in pendingEntries) {
            if (entry is GroupConfigEntry) {
                list.add(entry as GroupConfigEntry)
            }
        }
        return list
    }


    override fun close() {
        try {
            entriesFile.close()
            entryIndexFile.close()
        } catch (e: IOException) {
            throw LogException("failed to close", e)
        }
    }
}

