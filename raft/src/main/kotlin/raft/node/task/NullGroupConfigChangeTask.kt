package raft.node.task

import raft.node.NodeId

class NullGroupConfigChangeTask : GroupConfigChangeTask {
    override fun isTargetNode(nodeId: NodeId?): Boolean {
        return false
    }

    override fun onLogCommitted() {}
    @Throws(Exception::class)
    override fun call(): GroupConfigChangeTaskResult? {
        return null
    }

    override fun toString(): String {
        return "NullGroupConfigChangeTask{}"
    }
}