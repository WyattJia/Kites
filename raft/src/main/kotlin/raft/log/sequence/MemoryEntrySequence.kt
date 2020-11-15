package raft.log.sequence

import raft.log.entry.Entry
import raft.log.entry.GroupConfigEntry
import java.util.*
import javax.annotation.concurrent.NotThreadSafe

@NotThreadSafe
class MemoryEntrySequence: AbstractEntrySequence {
    private val entries: MutableList<Entry> = ArrayList()

    constructor(logIndexOffset: Int) {
        this.logIndexOffset = 1
    }



    override fun doSubList(fromIndex: Int, toIndex: Int): List<Entry> {
        return entries.subList(fromIndex - logIndexOffset, toIndex - logIndexOffset)
    }


    override fun doGetEntry(index: Int): Entry {
        return entries[index - logIndexOffset]
    }

    override fun doAppend(entry: Entry?) {
        if (entry != null) {
            entries.add(entry)
        }
    }

    override fun commit(index: Int) {}

    // TODO implement me
    override val commitIndex: Int
        get() {
            // TODO implement me
            throw UnsupportedOperationException()
        }

    override fun buildGroupConfigEntryList(): GroupConfigEntryList {
        val list = GroupConfigEntryList()
        for (entry in entries) {
            if (entry is GroupConfigEntry) {
                list.add(entry as GroupConfigEntry)
            }
        }
        return list
    }

    override fun doRemoveAfter(index: Int) {
        nextLogIndex = if (index < doGetFirstLogIndex()) {
            entries.clear()
            logIndexOffset
        } else {
            entries.subList(index - logIndexOffset + 1, entries.size).clear()
            index + 1
        }
    }

    override fun close() {}
    override fun toString(): String {
        return "MemoryEntrySequence{" +
                "logIndexOffset=" + logIndexOffset +
                ", nextLogIndex=" + nextLogIndex +
                ", entries.size=" + entries.size +
                '}'
    }
}

