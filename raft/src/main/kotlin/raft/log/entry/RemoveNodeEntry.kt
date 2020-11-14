package raft.log.entry

import raft.log.entry.Entry.Companion.KIND_REMOVE_NODE
import raft.node.NodeEndpoint
import raft.node.NodeId
import java.util.stream.Collectors


class RemoveNodeEntry(index: Int, term: Int, nodeEndpoints: Set<NodeEndpoint?>?, nodeToRemove: NodeId) :
    GroupConfigEntry(KIND_REMOVE_NODE, index, term, (nodeEndpoints)!! as Set<NodeEndpoint>) {
    private val nodeToRemove: NodeId
    override val resultNodeEndpoints: Set<Any>
        get() = getNodeEndpoints().stream()
            .filter { c -> !c.id.equals(nodeToRemove) }
            .collect(Collectors.toSet())

    fun getNodeToRemove(): NodeId {
        return nodeToRemove
    }

    override val commandBytes: ByteArray
        get() {
            return Protos.RemoveNodeCommand.newBuilder()
                .addAllNodeEndpoints(getNodeEndpoints().stream().map { c ->
                    Protos.NodeEndpoint.newBuilder()
                        .setId(c.getId().getValue())
                        .setHost(c.getHost())
                        .setPort(c.getPort())
                        .build()
                }.collect(Collectors.toList()))
                .setNodeToRemove(nodeToRemove.getValue())
                .build().toByteArray()
        }

    override fun toString(): String {
        return ("RemoveNodeEntry{" +
                "index=" + index +
                ", term=" + term +
                ", nodeEndpoints=" + getNodeEndpoints() +
                ", nodeToRemove=" + nodeToRemove +
                '}')
    }

    init {
        this.nodeToRemove = nodeToRemove
    }
}

