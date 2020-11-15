package raft.log

import org.slf4j.LoggerFactory
import raft.log.entry.Entry
import raft.log.entry.EntryMeta
import raft.log.sequence.EntrySequence
import raft.log.sequence.MemoryEntrySequence
import raft.log.snapshot.*
import raft.node.NodeEndpoint
import raft.rpc.message.InstallSnapshotRpc
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.annotation.concurrent.NotThreadSafe


@NotThreadSafe
class MemoryLog(snapshot: Snapshot?, entrySequence: EntrySequence? ) :
    AbstractLog() {
    constructor() : this(EmptySnapshot(), MemoryEntrySequence(1)) {
    }

    override fun generateSnapshot(lastAppliedEntryMeta: EntryMeta?, groupConfig: Set<NodeEndpoint?>?): Snapshot? {
        val output = ByteArrayOutputStream()
        try {
            stateMachine.generateSnapshot(output)
        } catch (e: IOException) {
            throw LogException("failed to generate snapshot", e)
        }
        return MemorySnapshot(
            lastAppliedEntryMeta!!.index,
            lastAppliedEntryMeta.term,
            output.toByteArray(),
            groupConfig as Set<NodeEndpoint>
        )
    }

    override fun newSnapshotBuilder(firstRpc: InstallSnapshotRpc?): SnapshotBuilder<Snapshot?> {
        return firstRpc?.let { MemorySnapshotBuilder(it) } as SnapshotBuilder<Snapshot?>
    }

    override fun replaceSnapshot(newSnapshot: Snapshot?) {
        val logIndexOffset: Int = newSnapshot!!.lastIncludedIndex + 1
        val newEntrySequence: EntrySequence = MemoryEntrySequence(1)
        val remainingEntries: List<Entry> = entrySequence!!.subView(logIndexOffset)
        newEntrySequence.append(remainingEntries)
        logger.debug("snapshot -> {}", newSnapshot)
        snapshot = newSnapshot
        logger.debug("entry sequence -> {}", newEntrySequence)
        entrySequence = newEntrySequence
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MemoryLog::class.java)
    }

    init {
        this.snapshot = snapshot
        this.entrySequence = entrySequence
    }
}


