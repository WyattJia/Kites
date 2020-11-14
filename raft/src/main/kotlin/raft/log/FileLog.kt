package raft.log

import com.google.common.eventbus.EventBus
import raft.log.entry.Entry
import raft.log.sequence.EntrySequence
import raft.log.sequence.FileEntrySequence
import raft.log.snapshot.FileSnapshot
import raft.log.snapshot.Snapshot
import raft.node.NodeEndpoint
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

    override fun installSnapshot(rpc: InstallSnapshotRpc?): InstallSnapshotState? {
        TODO("Not yet implemented")
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

    /**
     * Set state machine.
     *
     *
     * It will be called when
     *
     *  * apply the log entry
     *  * generate snapshot
     *  * apply snapshot
     *
     *
     * @param stateMachine state machine
     */
    override fun setStateMachine(stateMachine: Any) {
        TODO("Not yet implemented")
    }


    protected fun replaceSnapshot(newSnapshot: Snapshot) {
        val fileSnapshot: FileSnapshot = newSnapshot as FileSnapshot
        val lastIncludedIndex: Int = fileSnapshot.lastIncludedIndex
        val logIndexOffset = lastIncludedIndex + 1
        val remainingEntries: List<Entry> = entrySequence!!.subView(logIndexOffset)
        val newEntrySequence: EntrySequence = FileEntrySequence(fileSnapshot.logDir!!, logIndexOffset)
        newEntrySequence.append(remainingEntries)
        newEntrySequence.commit(Math.max(commitIndex, lastIncludedIndex))
        newEntrySequence.close()
        snapshot?.close()
        entrySequence!!.close()
        newSnapshot.close()
        val generation = rootDir.rename(fileSnapshot.logDir!!, lastIncludedIndex)
        snapshot = FileSnapshot(generation)
        entrySequence = FileEntrySequence(generation, logIndexOffset)
        groupConfigEntryList = (entrySequence as FileEntrySequence).buildGroupConfigEntryList()
        commitIndex = (entrySequence as FileEntrySequence).commitIndex
    }
}
