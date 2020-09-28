package raft.rpc.message

import com.google.common.base.Preconditions
import raft.node.NodeId
import javax.annotation.Nonnull

class AppendEntriesResultMessage(result: AppendEntriesResult, sourceNodeId: NodeId, @Nonnull rpc: AppendEntriesRpc) {
    private val result: AppendEntriesResult
    private val sourceNodeId: NodeId

    // TODO remove rpc, just lastEntryIndex required, or move to replicating state?
    val rpc: AppendEntriesRpc

    fun get(): AppendEntriesResult {
        return result
    }

    fun getSourceNodeId(): NodeId {
        return sourceNodeId
    }

    init {
        Preconditions.checkNotNull<Any>(rpc)
        this.result = result
        this.sourceNodeId = sourceNodeId
        this.rpc = rpc
    }
}
