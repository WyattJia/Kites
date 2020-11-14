package raft.log.snapshot

import raft.log.LogException
import raft.rpc.message.InstallSnapshotRpc
import java.io.ByteArrayOutputStream
import java.io.IOException


class MemorySnapshotBuilder(firstRpc: InstallSnapshotRpc) : AbstractSnapshotBuilder<MemorySnapshot?>(firstRpc) {
    private val output: ByteArrayOutputStream = ByteArrayOutputStream()

    @Throws(IOException::class)
    override fun doWrite(data: ByteArray?) {
        output.write(data!!)
    }

    override fun build(): MemorySnapshot {
        return MemorySnapshot(lastIncludedIndex, lastIncludedTerm, output.toByteArray(), lastConfig)
    }

    override fun close() {}

    init {
        try {
            output.write(firstRpc.data!!)
        } catch (e: IOException) {
            throw LogException(e)
        }
    }
}

