package raft.node.store

import raft.node.NodeId
import raft.support.Files
import raft.support.RandomAccessFileAdapter
import raft.support.SeekableFile
import java.io.File
import java.io.IOException
import javax.annotation.concurrent.NotThreadSafe

@NotThreadSafe
class FileNodeStore(var file: File) : NodeStore {

    private val seekableFile:SeekableFile
    private var term = 0
    private lateinit var votedFor: NodeId
    override fun getVotedFor(): NodeId {
        return votedFor
    }

    init {
        try {
            if (!file.exists()) {
                Files.touch(file)
            }
            seekableFile = RandomAccessFileAdapter(file)
            initializeOrLoad()
        } catch (e: IOException) {
            throw NodeStoreException(e)
        }


    }

//    constructor(seekableFile: SeekableFile) {
//        try {
//            initializeOrLoad()
//        } catch (e: IOException) {
//            throw NodeStoreException(e)
//        }
//    }




    @Throws(IOException::class)
    private fun initializeOrLoad() {
        if (seekableFile.size() == 0.toLong()) {
            // (term, 4) + (votedFor length, 4) = 8
            seekableFile.truncate(8L)
            seekableFile.seek(0)
            seekableFile.writeInt(0) // term
            seekableFile.writeInt(0) // votedFor length
        } else {
            // read term
            term = seekableFile.readInt()
            // read voted for
            val length = seekableFile.readInt()
            if (length > 0) {
                val bytes = ByteArray(length)
                seekableFile.read(bytes)
                votedFor = NodeId(String(bytes))
            }
        }
    }

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





    override fun setVotedFor(votedFor: NodeId) {
        try {
            seekableFile.seek(OFFSET_VOTED_FOR)
            val bytes: ByteArray = votedFor.getValue().toByteArray()
            seekableFile.writeInt(bytes.size)
            seekableFile.write(bytes)
        } catch (e: IOException) {
            throw NodeStoreException(e)
        }
        this.votedFor = votedFor
    }

    override fun close() {
        try {
            seekableFile.close()
        } catch (e: IOException) {
            throw NodeStoreException(e)
        }
    }

    companion object {
        const val FILE_NAME = "node.bin"
        private const val OFFSET_TERM: Long = 0
        private const val OFFSET_VOTED_FOR: Long = 4
    }
}
