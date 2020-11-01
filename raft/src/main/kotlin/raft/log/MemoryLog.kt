package raft.log

import com.google.common.eventbus.EventBus
import raft.log.sequence.EntrySequence
import raft.node.NodeEndpoint.NodeEndpoint


class MemoryLog internal constructor(entrySequence: EntrySequence?, override val nextIndex: Int, eventBus: EventBus)
    : AbstractLog(eventBus
) {

    init {
        this.entrySequence = entrySequence
    }

    /**
     * Generate snapshot.
     *
     * @param lastIncludedIndex last included index
     * @param groupConfig       group config
     */
    override fun generateSnapshot(lastIncludedIndex: Int, groupConfig: Set<NodeEndpoint?>?) {
        TODO("Not yet implemented")
    }

}
