package raft.log.snapshot

import java.io.InputStream


// TODO add doc
interface Snapshot {
    val lastIncludedIndex: Int
    val lastIncludedTerm: Int

    val lastConfig: Set<Any?>?
    val dataSize: Long

    fun readData(offset: Int, length: Int): SnapshotChunk?

    val dataStream: InputStream?

    fun close()
}

