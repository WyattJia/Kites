package raft.log.snapshot

import raft.node.NodeEndpoint
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.annotation.concurrent.Immutable

@Immutable
class MemorySnapshot constructor(
    override val lastIncludedIndex: Int,
    override val lastIncludedTerm: Int,
    val data: ByteArray = ByteArray(0),
    override val lastConfig: Set<NodeEndpoint> = emptySet()
) :
    Snapshot {

    override val dataSize: Long
        get() = data.size.toLong()

    override fun readData(offset: Int, length: Int): SnapshotChunk {
        if (offset < 0 || offset > data.size) {
            throw IndexOutOfBoundsException("offset $offset out of bound")
        }
        val bufferLength = Math.min(data.size - offset, length)
        val buffer = ByteArray(bufferLength)
        System.arraycopy(data, offset, buffer, 0, bufferLength)
        return SnapshotChunk(buffer, offset + length >= data.size)
    }

    override val dataStream: InputStream
        get() = ByteArrayInputStream(data)

    override fun close() {}
    override fun toString(): String {
        return "MemorySnapshot{" +
                "lastIncludedIndex=" + lastIncludedIndex +
                ", lastIncludedTerm=" + lastIncludedTerm +
                ", data.size=" + data.size +
                '}'
    }
}


