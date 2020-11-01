package raft.support

import java.io.*


class RandomAccessFileAdapter @JvmOverloads constructor(private val file: File?, mode: String? = "rw") : SeekableFile {

    private val randomAccessFile: RandomAccessFile = RandomAccessFile(file, mode)

    @Throws(IOException::class)
    override fun seek(position: Long) {
        randomAccessFile.seek(position)
    }

    @Throws(IOException::class)
    override fun writeInt(i: Int) {
        randomAccessFile.writeInt(i)
    }

    @Throws(IOException::class)
    override fun writeLong(l: Long) {
        randomAccessFile.writeLong(l)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray?) {
        randomAccessFile.write(b)
    }

    @Throws(IOException::class)
    override fun readInt(): Int {
        return randomAccessFile.readInt()
    }

    @Throws(IOException::class)
    override fun readLong(): Long {
        return randomAccessFile.readLong()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray?): Int {
        return randomAccessFile.read(b)
    }

    @Throws(IOException::class)
    override fun size(): Long {
        return randomAccessFile.length()
    }

    @Throws(IOException::class)
    override fun truncate(size: Long) {
        randomAccessFile.setLength(size)
    }

    @Throws(IOException::class)
    override fun inputStream(start: Long): InputStream {
        val input = FileInputStream(file)
        if (start > 0) {
            input.skip(start)
        }
        return input
    }

    @Throws(IOException::class)
    override fun position(): Long {
        return randomAccessFile.filePointer
    }

    @Throws(IOException::class)
    override fun flush() {
    }

    @Throws(IOException::class)
    override fun close() {
        randomAccessFile.close()
    }

}
