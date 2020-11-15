package raft.log

import raft.log.entry.Entry
import raft.log.entry.EntryMeta
import raft.log.sequence.EntrySequence
import raft.log.sequence.FileEntrySequence
import raft.log.snapshot.*
import raft.node.NodeEndpoint
import raft.rpc.message.InstallSnapshotRpc
import java.io.File
import java.io.IOException
import javax.annotation.concurrent.NotThreadSafe


@NotThreadSafe
class FileLog(baseDir: File?) : AbstractLog() {
    private val rootDir: RootDir = RootDir(baseDir!!)
    override fun generateSnapshot(
        lastAppliedEntryMeta: EntryMeta?,
        groupConfig: Set<NodeEndpoint?>?
    ): Snapshot? {
        val logDir: LogDir = rootDir.logDirForGenerating
        try {
            FileSnapshotWriter(
                logDir.snapshotFile, lastAppliedEntryMeta!!.index, lastAppliedEntryMeta.term, groupConfig as Set<NodeEndpoint>
            ).use { snapshotWriter ->
                stateMachine.generateSnapshot(
                    snapshotWriter.getOutput()
                )
            }
        } catch (e: IOException) {
            throw LogException("failed to generate snapshot", e)
        }
        return FileSnapshot(logDir)
    }


    override fun newSnapshotBuilder(firstRpc: InstallSnapshotRpc?): SnapshotBuilder<Snapshot?> {
        return FileSnapshotBuilder(firstRpc!!, rootDir.logDirForInstalling) as SnapshotBuilder<Snapshot?>
    }

    override fun replaceSnapshot(newSnapshot: Snapshot?) {
        val fileSnapshot = newSnapshot as FileSnapshot
        val lastIncludedIndex: Int = fileSnapshot.lastIncludedIndex
        val logIndexOffset = lastIncludedIndex + 1
        val remainingEntries: List<Entry?> = entrySequence!!.subView(logIndexOffset)
        val newEntrySequence: EntrySequence = FileEntrySequence(fileSnapshot.logDir!!, logIndexOffset)
        newEntrySequence.append(remainingEntries)
        newEntrySequence.commit(Math.max(commitIndex, lastIncludedIndex))
        newEntrySequence.close()
        snapshot!!.close()
        entrySequence!!.close()
        newSnapshot.close()
        val generation = rootDir.rename(fileSnapshot.logDir!!, lastIncludedIndex)
        snapshot = FileSnapshot(generation)
        entrySequence = FileEntrySequence(generation, logIndexOffset)
        groupConfigEntryList = entrySequence!!.buildGroupConfigEntryList()!!
        commitIndex = entrySequence!!.commitIndex
    }

    init {
        val latestGeneration: LogGeneration = rootDir.latestGeneration!!
        snapshot = EmptySnapshot()
        // TODO add log
        if (latestGeneration.snapshotFile.exists()) {
            snapshot = FileSnapshot(latestGeneration)
        }
        val fileEntrySequence = FileEntrySequence(latestGeneration, snapshot!!.lastIncludedIndex + 1)
        commitIndex = fileEntrySequence.commitIndex
        entrySequence = fileEntrySequence
        // TODO apply last group config entry
        groupConfigEntryList = entrySequence!!.buildGroupConfigEntryList()!!
    }
}
