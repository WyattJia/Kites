package raft.log.event

import raft.log.entry.GroupConfigEntry

class GroupConfigEntryFromLeaderAppendEvent(entry: GroupConfigEntry?) :
    AbstractEntryEvent<GroupConfigEntry?>(entry)
