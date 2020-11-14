package raft.log.entry

import raft.node.NodeEndpoint

abstract class GroupConfigEntry protected constructor(
    kind: Int,
    index: Int,
    term: Int,
    nodeEndpoints: Set<NodeEndpoint>
) :
    AbstractEntry(kind, index, term) {
    private val nodeEndpoints: Set<NodeEndpoint> = nodeEndpoints
    fun getNodeEndpoints(): Set<NodeEndpoint> {
        return nodeEndpoints
    }

    abstract val resultNodeEndpoints: Set<Any?>?

}

