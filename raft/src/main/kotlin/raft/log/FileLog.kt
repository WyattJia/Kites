package raft.log

import com.google.common.eventbus.EventBus
import raft.log.sequence.FileEntrySequence
import raft.node.NodeEndpoint.NodeEndpoint
import java.io.File


class FileLog(baseDir: File, eventBus: EventBus, override val nextIndex: Int) : AbstractLog() {
    private val rootDir: RootDir = RootDir(baseDir)

    init {
        val lastestGeneration: LogGeneration = rootDir.latestGeneration!!
        entrySequence = FileEntrySequence(
                // todo review those two args, check correct.
                lastestGeneration.lastIncludedIndex, lastestGeneration.lastIncludedIndex
        )
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
