package raft.log.sequence

import raft.log.entry.Entry
import raft.log.entry.EntryFactory
import raft.support.RandomAccessFileAdapter
import raft.support.SeekableFile
import java.io.File
import java.io.IOException

class EntriesFile(seekableFile: SeekableFile) {

    private val seekableFile: SeekableFile = seekableFile

    constructor(file: File?) : this(RandomAccessFileAdapter(file)) {}

    @Throws(IOException::class)
    fun appendEntry(entry: Entry): Long {
        val offset: Long = seekableFile.size()
        seekableFile.seek(offset)
        seekableFile.writeInt(entry.kind)
        seekableFile.writeInt(entry.index)
        seekableFile.writeInt(entry.term)
        val commandBytes: ByteArray? = entry.commandBytes
        if (commandBytes != null) {
            seekableFile.writeInt(commandBytes.size)
        }
        seekableFile.write(commandBytes)
        return offset
    }

    @Throws(IOException::class)
    fun loadEntry(offset: Long, factory: EntryFactory): Entry {
        require(offset <= seekableFile.size()) { "offset > size" }
        seekableFile.seek(offset)
        val kind: Int = seekableFile.readInt()
        val index: Int = seekableFile.readInt()
        val term: Int = seekableFile.readInt()
        val length: Int = seekableFile.readInt()
        val bytes = ByteArray(length)
        seekableFile.read(bytes)
        return factory.create(kind, index, term, bytes)
    }

    @Throws(IOException::class)
    fun size(): Long {
        return seekableFile.size()
    }

    @Throws(IOException::class)
    fun clear() {
        truncate(0L)
    }

    @Throws(IOException::class)
    fun truncate(offset: Long) {
        seekableFile.truncate(offset)
    }

    @Throws(IOException::class)
    fun close() {
        seekableFile.close()
    }

}

