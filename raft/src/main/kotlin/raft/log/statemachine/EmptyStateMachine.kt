package raft.log.statemachine

import raft.log.snapshot.Snapshot
import java.io.IOException
import java.io.OutputStream


class EmptyStateMachine :StateMachine {

    override var lastApplied = 0


    override fun applyLog(context: StateMachineContext?, index: Int, commandBytes: ByteArray, firstLogIndex: Int) {
        lastApplied = index
    }

    override fun shouldGenerateSnapshot(firstLogIndex: Int, lastApplied: Int): Boolean {
        return false
    }

    @Throws(IOException::class)
    override fun generateSnapshot(output: OutputStream?) {
    }

    @Throws(IOException::class)
    override fun applySnapshot(snapshot: Snapshot) {
        lastApplied = snapshot.lastIncludedIndex
    }

    override fun shutdown() {}

}

