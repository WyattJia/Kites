package raft.log

import com.google.common.eventbus.EventBus
import raft.node.NodeEndpoint.NodeEndpoint


class MemoryLog : AbstractLog() {
    override  var eventBus:EventBus = EventBus()

    override val nextIndex: Int = 0
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
