package raft.node.store

import raft.node.NodeId.NodeId
import raft.support.Files
import raft.support.RandomAccessFileAdapter
import raft.support.SeekableFile
import java.io.File
import java.io.IOException

abstract class FileNodeStore(private var seekableFile: SeekableFile): NodeStore {

    val FILE_NAME:String = "node.bin"
    private val OFFSET_TERM:Long = 0
    private val OFFSET_VOTED_FOR:Long = 4

    private var term:Int = 0
    private var votedFor:NodeId? = null

    constructor(file: File) {
        try {
            if (!file.exists()) {
                Files.touch(file)
            }

            seekableFile = RandomAccessFileAdapter(file)
            initializeOrLoad();
        } catch (e: IOException) {
            throw NodeStoreException(e)
        }
    }

    // for test
    // constructor(seekableFile: SeekableFile) {
    //     this.seekableFile = seekableFile
    //     try {
    //         initializeOrLoad()
    //     } catch (e: IOException) {
    //         throw NodeStoreException(e)
    //     }
    // }

    @Throws(IOException::class)
    private fun initializeOrLoad(){
        if (this.seekableFile.size() == 0.toLong()) {
            seekableFile.truncate(8L)
            seekableFile.seek(0)
            seekableFile.writeInt(0)
            seekableFile.writeInt(0)
        } else {
            var term = seekableFile.readInt()
            val length = seekableFile.readInt()

            if (length > 0) {
                val bytes: ByteArray = byteArrayOf(length.toByte())
                seekableFile.read(bytes)
                this.votedFor = NodeId(bytes.toString())
            }
        }
    }

    abstract fun NodeStoreException(e: IOException): Throwable



    override fun getTerm(): Int {
        return term
    }

    override fun setTerm(term: Int) {
        try {
            seekableFile.seek(OFFSET_TERM)
            seekableFile.writeInt(term)
        } catch (e: IOException) {
            throw NodeStoreException(e)
        }
        this.term = term
    }

    override fun getVotedFor(): NodeId {
        return votedFor!!
    }

    override fun setVotedFor(votedFor: NodeId) {
        try {
            seekableFile.seek(OFFSET_VOTED_FOR)
            if (votedFor == null) {
                seekableFile.writeInt(0)
                seekableFile.truncate(8L)
            } else {
                val bytes = votedFor.getValue().toByteArray()
                seekableFile.writeInt(bytes.size)
            }
        } catch (e: IOException) {
            throw NodeStoreException(e)
        }
        this.votedFor = votedFor
    }

    override fun close() {
        try {
            seekableFile.close()
        } catch (e:IOException) {
            throw NodeStoreException(e)
        }
    }
}
