package raft.rpc.message

import com.google.common.base.Preconditions
import raft.node.NodeId

class InstallSnapshotResultMessage(
    result: InstallSnapshotResult,
    sourceNodeId: NodeId,
    rpc: InstallSnapshotRpc
) {
    private val result: InstallSnapshotResult
    private val sourceNodeId: NodeId
    val rpc: InstallSnapshotRpc

    fun get(): InstallSnapshotResult {
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

