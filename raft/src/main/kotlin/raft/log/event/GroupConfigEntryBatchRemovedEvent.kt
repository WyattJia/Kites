package raft.log.event

import raft.log.entry.GroupConfigEntry


class GroupConfigEntryBatchRemovedEvent(firstRemovedEntry: GroupConfigEntry) {
    private val firstRemovedEntry: GroupConfigEntry
    fun getFirstRemovedEntry(): GroupConfigEntry {
        return firstRemovedEntry
    }

    init {
        this.firstRemovedEntry = firstRemovedEntry
    }
}
