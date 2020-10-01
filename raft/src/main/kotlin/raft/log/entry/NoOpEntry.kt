package raft.log.entry

import raft.log.entry.Entry.Companion.KIND_NO_OP

class NoOpEntry(index: Int, term: Int) : AbstractEntry(KIND_NO_OP, index, term) {
    override val commandBytes: ByteArray
        get() = ByteArray(0)

    override fun toString(): String {
        return "NoOpEntry{" +
                "index=" + index +
                ", term=" + term +
                '}'
    }
}
