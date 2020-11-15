package raft.log.snapshot

import raft.log.LogDir
import raft.log.LogException
import raft.node.NodeEndpoint
import raft.support.RandomAccessFileAdapter
import raft.support.SeekableFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.annotation.Nonnull


class FileSnapshot : Snapshot {
    var logDir: LogDir? = null
        private set
    private var seekableFile: SeekableFile? = null
    override var lastIncludedIndex = 0
        private set
    override var lastIncludedTerm = 0
        private set

    override var lastConfig: Set<NodeEndpoint>? = null
        private set
    private var dataStart: Long = 0
    override var dataSize: Long = 0
        private set

    constructor(logDir: LogDir) {
        this.logDir = logDir
        logDir.snapshotFile?.let { readHeader(it) }
    }

    constructor(file: File) {
        readHeader(file)
    }

    constructor(seekableFile: SeekableFile) {
        readHeader(seekableFile)
    }

    private fun readHeader(file: File) {
        try {
            readHeader(RandomAccessFileAdapter(file, "r"))
        } catch (e: FileNotFoundException) {
            throw LogException(e)
        }
    }

    private fun readHeader(seekableFile: SeekableFile) {
        this.seekableFile = seekableFile
//        try {
//            val headerLength = seekableFile.readInt()
//            val headerBytes = ByteArray(headerLength)
//            seekableFile.read(headerBytes)
//            val header: Protos.SnapshotHeader = Protos.SnapshotHeader.parseFrom(headerBytes)
//            lastIncludedIndex = header.getLastIndex()
//            lastIncludedTerm = header.getLastTerm()
//            lastConfig = header.getLastConfigList().stream()
//                .map { e -> NodeEndpoint(e.getId(), e.getHost(), e.getPort()) }
//                .collect(Collectors.toSet())
//            dataStart = seekableFile.position()
//            dataSize = seekableFile.size() - dataStart
//        } catch (e: InvalidProtocolBufferException) {
//            throw LogException("failed to parse header of snapshot", e)
//        } catch (e: IOException) {
//            throw LogException("failed to read snapshot", e)
//        }
    }

    @Nonnull
    override fun readData(offset: Int, length: Int): SnapshotChunk {
        require(offset <= dataSize) { "offset > data length" }
        return try {
            seekableFile!!.seek(dataStart + offset)
            val buffer = ByteArray(Math.min(length, dataSize.toInt() - offset))
            val n = seekableFile!!.read(buffer)
            SnapshotChunk(buffer, offset + n >= dataSize)
        } catch (e: IOException) {
            throw LogException("failed to seek or read snapshot content", e)
        }
    }

    @get:Nonnull
    override val dataStream: InputStream?
        get() = try {
            seekableFile!!.inputStream(dataStart)
        } catch (e: IOException) {
            throw LogException("failed to get input stream of snapshot data", e)
        }

    override fun close() {
        try {
            seekableFile!!.close()
        } catch (e: IOException) {
            throw LogException("failed to close file", e)
        }
    }
}

