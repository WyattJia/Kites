package raft.log.entry

abstract class AbstractEntry(override val kind: Int, override val index: Int, override val term: Int) : Entry {
    override val meta: EntryMeta
        get() = EntryMeta(kind, index, term)

}

