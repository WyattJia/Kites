package raft.log.snapshot


class SnapshotChunk internal constructor(private val bytes: ByteArray, val isLastChunk: Boolean) {

    fun toByteArray(): ByteArray {
        return bytes
    }
}

