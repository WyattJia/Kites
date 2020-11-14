package raft.log.entry

import raft.log.entry.Entry.Companion.KIND_ADD_NODE
import raft.node.NodeEndpoint
import java.util.stream.Collectors


class AddNodeEntry(index: Int, term: Int, nodeEndpoints: Set<NodeEndpoint?>?, newNodeEndpoint: NodeEndpoint) :
    GroupConfigEntry(KIND_ADD_NODE, index, term, (nodeEndpoints)!! as Set<NodeEndpoint>) {
    private val newNodeEndpoint: NodeEndpoint
    fun getNewNodeEndpoint(): NodeEndpoint {
        return newNodeEndpoint
    }

    override val resultNodeEndpoints: Set<Any?>
        get() {
            val configs: HashSet<Any?> = HashSet<Any?>(getNodeEndpoints())
            configs.add(newNodeEndpoint)
            return configs
        }
    override val commandBytes: ByteArray
        get() = Protos.AddNodeCommand.newBuilder()
            .addAllNodeEndpoints(getNodeEndpoints().stream().map { c ->
                Protos.NodeEndpoint.newBuilder()
                    .setId(c.getId().getValue())
                    .setHost(c.getHost())
                    .setPort(c.getPort())
                    .build()
            }.collect(Collectors.toList()))
            .setNewNodeEndpoint(
                Protos.NodeEndpoint.newBuilder()
                    .setId(newNodeEndpoint.getId().getValue())
                    .setHost(newNodeEndpoint.getHost())
                    .setPort(newNodeEndpoint.getPort())
                    .build()
            ).build().toByteArray()

    override fun toString(): String {
        return ("AddNodeEntry{" +
                "index=" + index +
                ", term=" + term +
                ", nodeEndpoints=" + getNodeEndpoints() +
                ", newNodeEndpoint=" + newNodeEndpoint +
                '}')
    }

    init {
        this.newNodeEndpoint = newNodeEndpoint
    }
}

