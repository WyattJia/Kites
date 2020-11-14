package raft.log.event

import raft.log.entry.Entry

abstract class AbstractEntryEvent<T : Entry?>(val entry: T)

