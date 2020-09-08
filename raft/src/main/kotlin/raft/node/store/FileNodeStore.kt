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

    @Throws(IOException::class)
    fun initializeOrLoad(){

    }

    abstract fun NodeStoreException(e: IOException): Throwable



    override fun getTerm(): Int {
        TODO("Not yet implemented")
    }

    override fun setTerm(term: Int) {
        TODO("Not yet implemented")
    }

    override fun getVotedFor(): NodeId {
        TODO("Not yet implemented")
    }

    override fun setVotedFor(votedFor: NodeId) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
