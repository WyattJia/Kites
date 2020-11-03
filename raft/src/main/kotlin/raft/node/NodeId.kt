package raft.node


class NodeId(private val value: String) {

    /**
     * Create.
     *
     * @param value value
     * @return node id
     */
    fun of(value: String): NodeId {
        return NodeId(value)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return true
    }

    fun getValue(): String {
        return value
    }

    override fun toString(): String {
        return "NodeId()"
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}