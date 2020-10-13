package raft.log.sequence

import raft.log.entry.Entry
import raft.log.entry.EntryMeta


interface EntrySequence {
    val isEmpty: Boolean
    val firstLogIndex: Int
    val lastLogIndex: Int
    val nextLogIndex: Int

    fun subView(fromIndex: Int): List<Entry>

    // [fromIndex, toIndex)
    fun subList(fromIndex: Int, toIndex: Int): List<Entry>


//    fun buildGroupConfigEntryList(): GroupConfigEntryList?


    fun isEntryPresent(index: Int): Boolean
    fun getEntryMeta(index: Int): EntryMeta
    fun getEntry(index: Int): Entry?
    val lastEntry: Entry?

    fun append(entry: Entry?)
    fun append(entries: List<Entry?>?)
    fun commit(index: Int)
    val commitIndex: Int

    fun removeAfter(index: Int)
    fun close()
}


