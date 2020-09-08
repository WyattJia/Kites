package raft.support

import java.io.IOException
import java.io.InputStream

interface SeekableFile {

    @Throws(IOException::class)
    fun position(): Long

    @Throws(IOException::class)
    fun seek(position: Long)

    @Throws(IOException::class)
    fun writeInt(i: Int)

    @Throws(IOException::class)
    fun writeLong(l: Long)

    @Throws(IOException::class)
    fun write(b: ByteArray?)

    @Throws(IOException::class)
    fun readInt(): Int

    @Throws(IOException::class)
    fun readLong(): Long

    @Throws(IOException::class)
    fun read(b: ByteArray?): Int

    @Throws(IOException::class)
    fun size(): Long

    @Throws(IOException::class)
    fun truncate(size: Long)

    @Throws(IOException::class)
    fun inputStream(start: Long): InputStream?

    @Throws(IOException::class)
    fun flush()

    @Throws(IOException::class)
    fun close()
}
