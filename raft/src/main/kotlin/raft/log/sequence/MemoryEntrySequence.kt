package raft.log.sequence

import raft.log.entry.Entry
import java.util.*


class MemoryEntrySequence(override var nextLogIndex: Int) : AbstractEntrySequence() {
    private val entries: MutableList<Entry> = ArrayList<Entry>()
    override fun doSubList(fromIndex: Int, toIndex: Int): List<Entry> {
        return entries.subList(fromIndex - logIndexOffset, toIndex - logIndexOffset)
    }

    override fun append(entry: Entry?) {
        TODO("Not yet implemented")
    }

    override fun append(entries: List<Entry?>?) {
        TODO("Not yet implemented")
    }

    override fun doGetEntry(index: Int): Entry {
        return entries[index - logIndexOffset]
    }

    protected override fun doAppend(entry: Entry) {
        entries.add(entry)
    }

    override fun commit(index: Int) {}

    override val commitIndex: Int
        get() {
            throw UnsupportedOperationException()
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


