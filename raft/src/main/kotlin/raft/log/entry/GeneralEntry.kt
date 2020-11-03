package raft.log.entry

import raft.log.entry.Entry.Companion.KIND_GENERAL

class GeneralEntry(index: Int, term: Int, override val commandBytes: ByteArray) :
        AbstractEntry(KIND_GENERAL, index, term) {

    override fun toString(): String {
        return "GeneralEntry{" +
                "index=" + index +
                ", term=" + term +
                '}'
    }
}