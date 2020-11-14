package raft.log.snapshot

import raft.log.LogException
import raft.node.NodeEndpoint
import raft.rpc.message.InstallSnapshotRpc
import java.io.IOException


abstract class AbstractSnapshotBuilder<T : Snapshot?>(firstRpc: InstallSnapshotRpc) :
    SnapshotBuilder<T> {
    var lastIncludedIndex: Int
    var lastIncludedTerm: Int
    var lastConfig: Set<NodeEndpoint>
    private var offset: Int
    protected fun write(data: ByteArray?) {
        try {
            doWrite(data)
        } catch (e: IOException) {
            throw LogException(e)
        }
    }

    @Throws(IOException::class)
    protected abstract fun doWrite(data: ByteArray?)


    override fun append(rpc: InstallSnapshotRpc?) {
        if (rpc != null) {
            require(!(rpc.offset != offset)) { "unexpected offset, expected " + offset + ", but was " + rpc.offset }
        }
        if (rpc != null) {
            require(!(rpc.lastIndex != lastIncludedIndex || rpc.lastTerm != lastIncludedTerm)) { "unexpected last included index or term" }
        }
        if (rpc != null) {
            write(rpc.data)
        }
        offset += rpc!!.getDataLength()
    }

    init {
        assert(firstRpc.offset == 0)
        lastIncludedIndex = firstRpc.lastIndex
        lastIncludedTerm = firstRpc.lastTerm
        lastConfig = firstRpc.lastConfig!!
        offset = firstRpc.getDataLength()
    }
}

