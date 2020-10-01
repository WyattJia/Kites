package raft.log.entry

interface Entry {
    val kind: Int
    val index: Int
    val term: Int
    val meta: EntryMeta?
    val commandBytes: ByteArray?

    companion object {
        const val KIND_NO_OP = 0
        const val KIND_GENERAL = 1
        const val KIND_ADD_NODE = 3
        const val KIND_REMOVE_NODE = 4
    }
}
