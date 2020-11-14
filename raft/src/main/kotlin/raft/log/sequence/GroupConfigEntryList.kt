package raft.log.sequence

import raft.log.entry.GroupConfigEntry
import java.util.*
import java.util.stream.Collectors
import javax.annotation.Nonnull
import javax.annotation.concurrent.NotThreadSafe

@NotThreadSafe
class GroupConfigEntryList : Iterable<GroupConfigEntry?> {
    private val entries = LinkedList<GroupConfigEntry>()
    val last: GroupConfigEntry?
        get() = if (entries.isEmpty()) null else entries.last

    fun add(entry: GroupConfigEntry) {
        entries.add(entry)
    }

    /**
     * Remove entries whose index is greater than `entryIndex`.
     *
     * @param entryIndex entry index
     * @return first removed entry, `null` if no entry removed
     */
    fun removeAfter(entryIndex: Int): GroupConfigEntry? {
        val iterator = entries.iterator()
        var firstRemovedEntry: GroupConfigEntry? = null
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.index > entryIndex) {
                if (firstRemovedEntry == null) {
                    firstRemovedEntry = entry
                }
                iterator.remove()
            }
        }
        return firstRemovedEntry
    }

    fun subList(fromIndex: Int, toIndex: Int): MutableList<Any>? {
        require(fromIndex <= toIndex) { "from index > to index" }
        return entries.stream()
            .filter { e: GroupConfigEntry -> e.index >= fromIndex && e.index < toIndex }
            .collect(Collectors.toList<Any>())
    }

    @Nonnull
    override fun iterator(): Iterator<GroupConfigEntry> {
        return entries.iterator()
    }

    override fun toString(): String {
        return "GroupConfigEntryList{$entries}"
    }
}

