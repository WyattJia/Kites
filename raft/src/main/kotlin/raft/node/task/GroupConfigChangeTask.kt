package raft.node.task

import raft.node.NodeId
import java.util.concurrent.Callable


interface GroupConfigChangeTask : Callable<GroupConfigChangeTaskResult?> {
    fun isTargetNode(nodeId: NodeId?): Boolean
    fun onLogCommitted()

    companion object {
        val NONE: GroupConfigChangeTask = NullGroupConfigChangeTask()
    }
}

