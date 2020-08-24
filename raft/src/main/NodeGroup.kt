class NodeGroup {
    val selfId = NodeId
    var memberMap = emptyMap<NodeId, GroupMember>()

    init {
        var endpoint = NodeEndpoint

        object endpoint {
            endpoint.getId()
        }
    }

    constructor()

    fun findMember(id: NodeId) : GroupMember? {
        var member: GroupMember? = getMember(id) ?: throw IllegalArgumentException("no such node $id")
        return member
    }

    fun getMember(id: NodeId): GroupMember? {
        return memberMap[id]
    }

    fun buildMemberMap (
        endpoints: Collection<NodeEndpoint>
    ) : Map<NodeId, GroupMember> {
        var map = hashMapOf<NodeId, GroupMember>()
        for (endpoint in endpoints) {
            map[endpoint.id] = GroupMember(endpoint)
        }
        if (map.isEmpty()) {
            throw IllegalArgumentException("endpoints is empty.")
        }
        return map
    }
}