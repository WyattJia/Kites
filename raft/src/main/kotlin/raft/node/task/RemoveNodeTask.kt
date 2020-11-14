package raft.node.task

import org.slf4j.LoggerFactory
import raft.node.NodeId

class RemoveNodeTask(context: GroupConfigChangeTaskContext, nodeId: NodeId) : AbstractGroupConfigChangeTask(context) {
    private val nodeId: NodeId = nodeId
    override fun isTargetNode(nodeId: NodeId?): Boolean {
        return this.nodeId.equals(nodeId)
    }

    protected override fun appendGroupConfig() {
        context.downgradeNode(nodeId)
    }

    @Synchronized
    override fun onLogCommitted() {
        check(state == State.GROUP_CONFIG_APPENDED) { "log committed before log appended" }
        this.state = State.GROUP_CONFIG_COMMITTED
        context.removeNode(nodeId)
        // todo
        Object().notify()
    }

    override fun toString(): String {
        return "RemoveNodeTask{" +
                "nodeId=" + nodeId +
                '}'
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoveNodeTask::class.java)
    }

}