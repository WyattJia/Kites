package raft.log.entry

class EntryFactory {
    fun create(kind: Int, index: Int, term: Int, commandBytes: ByteArray?): Entry {
        return try {
            when (kind) {
                Entry.KIND_NO_OP -> NoOpEntry(index, term)
                Entry.KIND_GENERAL -> GeneralEntry(index, term, commandBytes!!)

                else -> throw IllegalArgumentException("unexpected entry kind $kind")
            }
        } finally {
            print("Nothing")
        }

        // catch (e: InvalidProtocolBufferException) {
        //    throw IllegalStateException("failed to parse command", e)
        // }
    }
}


