package raft.log

import raft.log.entry.Entry

internal abstract class AbstractEntryEvent<T : Entry?>(val entry: T)

