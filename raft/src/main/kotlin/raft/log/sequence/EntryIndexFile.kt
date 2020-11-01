package raft.log.sequence

import raft.support.RandomAccessFileAdapter
import raft.support.SeekableFile
import java.io.File
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull


class EntryIndexFile(private val seekableFile: SeekableFile) : Iterable<EntryIndexItem?> {
    var entryIndexCount = 0
        private set
    private var minEntryIndex = 0
    private var maxEntryIndex = 0
    private val entryIndexMap: MutableMap<Int, EntryIndexItem> = java.util.HashMap()

    constructor(file: File?) : this(RandomAccessFileAdapter(file)) {}

    @Throws(IOException::class)
    private fun load() {
        if (seekableFile.size() == 0L) {
            entryIndexCount = 0
            return
        }
        minEntryIndex = seekableFile.readInt()
        maxEntryIndex = seekableFile.readInt()
        updateEntryIndexCount()
        var offset: Long
        var kind: Int
        var term: Int
        for (i in minEntryIndex..maxEntryIndex) {
            offset = seekableFile.readLong()
            kind = seekableFile.readInt()
            term = seekableFile.readInt()
            entryIndexMap[i] = EntryIndexItem(i, offset, kind, term)
        }
    }

    private fun updateEntryIndexCount() {
        entryIndexCount = maxEntryIndex - minEntryIndex + 1
    }

    val isEmpty: Boolean
        get() = entryIndexCount == 0

    fun getMinEntryIndex(): Int {
        checkEmpty()
        return minEntryIndex
    }

    private fun checkEmpty() {
        check(!isEmpty) { "no entry index" }
    }

    fun getMaxEntryIndex(): Int {
        checkEmpty()
        return maxEntryIndex
    }

    @Throws(IOException::class)
    fun appendEntryIndex(index: Int, offset: Long, kind: Int, term: Int) {
        if (seekableFile.size() == 0L) {
            seekableFile.writeInt(index)
            minEntryIndex = index
        } else {
            require(index == maxEntryIndex + 1) { "index must be " + (maxEntryIndex + 1) + ", but was " + index }
            seekableFile.seek(OFFSET_MAX_ENTRY_INDEX) // skip min entry index
        }

        // write max entry index
        seekableFile.writeInt(index)
        maxEntryIndex = index
        updateEntryIndexCount()

        // move to position after last entry offset
        seekableFile.seek(getOffsetOfEntryIndexItem(index))
        seekableFile.writeLong(offset)
        seekableFile.writeInt(kind)
        seekableFile.writeInt(term)
        entryIndexMap[index] = EntryIndexItem(index, offset, kind, term)
    }

    private fun getOffsetOfEntryIndexItem(index: Int): Long {
        return ((index - minEntryIndex) * LENGTH_ENTRY_INDEX_ITEM + Integer.BYTES * 2).toLong()
    }

    @Throws(IOException::class)
    fun clear() {
        seekableFile.truncate(0L)
        entryIndexCount = 0
        entryIndexMap.clear()
    }

    @Throws(IOException::class)
    fun removeAfter(newMaxEntryIndex: Int) {
        if (isEmpty || newMaxEntryIndex >= maxEntryIndex) {
            return
        }
        if (newMaxEntryIndex < minEntryIndex) {
            clear()
            return
        }
        seekableFile.seek(OFFSET_MAX_ENTRY_INDEX)
        seekableFile.writeInt(newMaxEntryIndex)
        seekableFile.truncate(getOffsetOfEntryIndexItem(newMaxEntryIndex + 1))
        for (i in newMaxEntryIndex + 1..maxEntryIndex) {
            entryIndexMap.remove(i)
        }
        maxEntryIndex = newMaxEntryIndex
        entryIndexCount = newMaxEntryIndex - minEntryIndex + 1
    }

    fun getOffset(entryIndex: Int): Long? {
        return get(entryIndex)?.offset
    }

    @Nonnull
    operator fun get(entryIndex: Int): EntryIndexItem? {
        checkEmpty()
        require(!(entryIndex < minEntryIndex || entryIndex > maxEntryIndex)) { "index < min or index > max" }
        return entryIndexMap[entryIndex]
    }

    @Nonnull
    override fun iterator(): Iterator<EntryIndexItem?> {
        return if (isEmpty) {
            Collections.emptyIterator()
        } else EntryIndexIterator(entryIndexCount, minEntryIndex)
    }

    @Throws(IOException::class)
    fun close() {
        seekableFile.close()
    }

    inner class EntryIndexIterator internal constructor(
        private val entryIndexCount: Int,
        private var currentEntryIndex: Int
    ) :
        MutableIterator<EntryIndexItem?> {
        override fun hasNext(): Boolean {
            checkModification()
            return currentEntryIndex <= maxEntryIndex
        }

        private fun checkModification() {
            check(this.entryIndexCount == this@EntryIndexFile.entryIndexCount) { "entry index count changed" }
        }

        override fun next(): EntryIndexItem {
            checkModification()
            return entryIndexMap[currentEntryIndex++]!!
        }

        /**
         * Removes from the underlying collection the last element returned by this iterator.
         */
        override fun remove() {
            TODO("Not yet implemented")
        }
    }

    companion object {
        private const val OFFSET_MAX_ENTRY_INDEX = Integer.BYTES.toLong()
        private const val LENGTH_ENTRY_INDEX_ITEM = 16
    }

    init {
        load()
    }
}


