package raft.log.entry

import raft.log.entry.Entry.Companion.KIND_ADD_NODE
import raft.node.NodeEndpoint


class AddNodeEntry(index: Int, term: Int, nodeEndpoints: Set<NodeEndpoint?>?, newNodeEndpoint: NodeEndpoint) :
    GroupConfigEntry(KIND_ADD_NODE, index, term, (nodeEndpoints)!! as Set<NodeEndpoint>) {
    private val newNodeEndpoint: NodeEndpoint = TODO()
    fun getNewNodeEndpoint(): NodeEndpoint {
        return newNodeEndpoint
    }

    override val resultNodeEndpoints: Set<Any?>
        get() {
            val configs: HashSet<Any?> = HashSet<Any?>(getNodeEndpoints())
            configs.add(newNodeEndpoint)
            return configs
        }
    override val commandBytes: ByteArray = TODO()
//        get() = Protos.AddNodeCommand.newBuilder()
//            .addAllNodeEndpoints(getNodeEndpoints().stream().map { c ->
//                Protos.NodeEndpoint.newBuilder(
//                    .setId(c.id.getValue())
//                    c.host)
//                    .setPort(c.port)
//                    .build()
//                )
//            }.collect(Collectors.toList()))
//            .setNewNodeEndpoint(
//                Protos.NodeEndpoint.newBuilder()
//                    .setId(newNodeEndpoint.getId().getValue())
//                    .setHost(newNodeEndpoint.getHost())
//                    .setPort(newNodeEndpoint.getPort())
//                    .build()
//            ).build().toByteArray()

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

