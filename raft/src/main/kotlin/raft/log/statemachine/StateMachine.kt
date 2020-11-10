package raft.log.statemachine

import raft.log.snapshot.Snapshot
import java.io.IOException
import java.io.OutputStream

/**
 * State machine.
 */
interface StateMachine {
    val lastApplied: Int

    fun applyLog(context: StateMachineContext?, index: Int, commandBytes: ByteArray, firstLogIndex: Int)

    /**
     * Should generate or not.
     *
     * @param firstLogIndex first log index in log files, may not be `0`
     * @param lastApplied   last applied log index
     * @return true if should generate, otherwise false
     */
    fun shouldGenerateSnapshot(firstLogIndex: Int, lastApplied: Int): Boolean

    /**
     * Generate snapshot to output.
     *
     * @param output output
     * @throws IOException if IO error occurred
     */
    @Throws(IOException::class)
    fun generateSnapshot(output: OutputStream?)

    @Throws(IOException::class)
    fun applySnapshot(snapshot: Snapshot)
    fun shutdown()
}

