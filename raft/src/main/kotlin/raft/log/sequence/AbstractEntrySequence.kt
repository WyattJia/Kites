package raft.log.sequence

import raft.log.entry.Entry
import raft.log.entry.EntryMeta

abstract class AbstractEntrySequence : EntrySequence {

    var logIndexOffset: Int = 0
    override var nextLogIndex: Int = 0
    override val isEmpty: Boolean
        get() = logIndexOffset == nextLogIndex
    override val firstLogIndex: Int
        get() {
            if (isEmpty) {
                throw EmptySequenceException()
            }
            return doGetFirstLogIndex()
        }

    fun doGetFirstLogIndex(): Int {
        return logIndexOffset
    }

    override val lastLogIndex: Int
        get() {
            if (isEmpty) {
                throw EmptySequenceException()
            }
            return doGetLastLogIndex()
        }

    fun doGetLastLogIndex(): Int {
        return nextLogIndex - 1
    }

    override fun isEntryPresent(index: Int): Boolean {
        return !isEmpty && index >= doGetFirstLogIndex() && index <= doGetLastLogIndex()
    }

    override fun getEntry(index: Int): Entry? {
        return if (!isEntryPresent(index)) {
            null
        } else doGetEntry(index)
    }

    override fun getEntryMeta(index: Int): EntryMeta? {
        val entry = getEntry(index)
        return entry?.meta
    }

    protected abstract fun doGetEntry(index: Int): Entry?
    override val lastEntry: Entry?
        get() = if (isEmpty) null else doGetEntry(doGetLastLogIndex())

    override fun subView(fromIndex: Int): List<Entry> {
        return if (isEmpty || fromIndex > doGetLastLogIndex()) {
            emptyList()
        } else subList(Math.max(fromIndex, doGetFirstLogIndex()), nextLogIndex)
    }

    // [fromIndex, toIndex)
    override fun subList(fromIndex: Int, toIndex: Int): List<Entry> {
        if (isEmpty) {
            throw EmptySequenceException()
        }
        require(!(fromIndex < doGetFirstLogIndex() || toIndex > doGetLastLogIndex() + 1 || fromIndex > toIndex)) { "illegal from index $fromIndex or to index $toIndex" }
        return doSubList(fromIndex, toIndex)
    }

    protected abstract fun doSubList(fromIndex: Int, toIndex: Int): List<Entry>
    override fun append(entries: List<Entry?>?) {
        if (entries != null) {
            for (entry in entries) {
                append(entry)
            }
        }
    }

    override fun append(entry: Entry?) {
        require(!(entry!!.index != nextLogIndex)) { "entry index must be $nextLogIndex" }
        doAppend(entry)
        nextLogIndex++
    }

    protected abstract fun doAppend(entry: Entry?)
    override fun removeAfter(index: Int) {
        if (isEmpty || index >= doGetLastLogIndex()) {
            return
        }
        doRemoveAfter(index)
    }

    protected abstract fun doRemoveAfter(index: Int)

    init {
        nextLogIndex = logIndexOffset
    }
}
