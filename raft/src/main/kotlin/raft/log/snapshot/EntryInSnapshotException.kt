package raft.log.snapshot

import raft.log.LogException


class EntryInSnapshotException(val index: Int) : LogException()

