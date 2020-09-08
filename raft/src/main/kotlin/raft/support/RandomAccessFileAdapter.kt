package raft.support

import java.io.File
import java.io.InputStream

class RandomAccessFileAdapter(private val file: File) : SeekableFile {
    override fun position(): Long {
        TODO("Not yet implemented")
    }

    override fun seek(position: Long) {
        TODO("Not yet implemented")
    }

    override fun writeInt(i: Int) {
        TODO("Not yet implemented")
    }

    override fun writeLong(l: Long) {
        TODO("Not yet implemented")
    }

    override fun write(b: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun readInt(): Int {
        TODO("Not yet implemented")
    }

    override fun readLong(): Long {
        TODO("Not yet implemented")
    }

    override fun read(b: ByteArray?): Int {
        TODO("Not yet implemented")
    }

    override fun size(): Long {
        TODO("Not yet implemented")
    }

    override fun truncate(size: Long) {
        TODO("Not yet implemented")
    }

    override fun inputStream(start: Long): InputStream? {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}