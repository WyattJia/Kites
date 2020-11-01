package raft.log.sequence

import raft.log.entry.EntryMeta
import javax.annotation.concurrent.Immutable

@Immutable
class EntryIndexItem(val index: Int, val offset: Long, val kind: Int, val term: Int) {

    fun toEntryMeta(): EntryMeta {
        return EntryMeta(kind, index, term)
    }
}
