package raft.log.snapshot

import raft.log.LogDir
import raft.log.LogException
import raft.rpc.message.InstallSnapshotRpc
import java.io.IOException


class FileSnapshotBuilder(firstRpc: InstallSnapshotRpc, logDir: LogDir) :
    AbstractSnapshotBuilder<FileSnapshot?>(firstRpc) {
    private val logDir: LogDir = logDir
    private var writer: FileSnapshotWriter? = null

    @Throws(IOException::class)
    override fun doWrite(data: ByteArray?) {
        writer?.write(data)
    }

    override fun build(): FileSnapshot {
        close()
        return FileSnapshot(logDir)
    }

    override fun close() {
        try {
            writer?.close()
        } catch (e: IOException) {
            throw LogException("failed to close writer", e)
        }
    }

    init {
        try {
            writer = firstRpc.lastConfig?.let {
                FileSnapshotWriter(
                    logDir.snapshotFile,
                    firstRpc.lastIndex,
                    firstRpc.lastTerm,
                    it
                )
            }
            writer?.write(firstRpc.data)
        } catch (e: IOException) {
            throw LogException("failed to write snapshot data to file", e)
        }
    }
}

