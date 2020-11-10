package raft.log.snapshot

import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.annotation.concurrent.Immutable


@Immutable
class EmptySnapshot : Snapshot {
    override val lastIncludedIndex: Int
        get() = 0
    override val lastIncludedTerm: Int
        get() = 0

    override val lastConfig: Set<Any>
        get() = emptySet()
    override val dataSize: Long
        get() = 0

    override fun readData(offset: Int, length: Int): SnapshotChunk {
        if (offset == 0) {
            return SnapshotChunk(ByteArray(0), true)
        }
        throw IllegalArgumentException("offset > 0")
    }

    override val dataStream: InputStream
        get() = ByteArrayInputStream(ByteArray(0))

    override fun close() {}
}

