package raft.log.event

import raft.log.entry.GroupConfigEntry

class GroupConfigEntryCommittedEvent(entry: GroupConfigEntry?) :
    AbstractEntryEvent<GroupConfigEntry?>(entry)
