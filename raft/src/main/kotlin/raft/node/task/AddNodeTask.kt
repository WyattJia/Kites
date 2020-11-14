package raft.node.task

import raft.node.NodeEndpoint
import raft.node.NodeId

class AddNodeTask(context: GroupConfigChangeTaskContext, endpoint: NodeEndpoint, nextIndex: Int, matchIndex: Int) :
    AbstractGroupConfigChangeTask(context) {
    private val endpoint: NodeEndpoint = endpoint
    private val nextIndex: Int = nextIndex
    private val matchIndex: Int = matchIndex

    constructor(
        context: GroupConfigChangeTaskContext,
        endpoint: NodeEndpoint,
        newNodeCatchUpTaskResult: NewNodeCatchUpTaskResult
    ) : this(context, endpoint, newNodeCatchUpTaskResult.nextIndex, newNodeCatchUpTaskResult.matchIndex) {
    }

    override fun isTargetNode(nodeId: NodeId?): Boolean {
        return endpoint.id.equals(nodeId)
    }

    protected override fun appendGroupConfig() {
        context.addNode(endpoint, nextIndex, matchIndex)
    }

    override fun toString(): String {
        return "AddNodeTask{" +
                "state=" + state +
                ", endpoint=" + endpoint +
                ", nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                '}'
    }

}