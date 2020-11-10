package raft.log.statemachine

import org.slf4j.LoggerFactory
import raft.log.snapshot.Snapshot
import raft.support.SingleThreadTaskExecutor
import raft.support.TaskExecutor
import java.io.IOException
import java.io.InputStream


/*
Todo: use kotlinx coroutines instead of single thread.
 */
abstract class AbstractSingleThreadStateMachine : StateMachine {
    @Volatile
    final override var lastApplied = 0
        private set
    private val taskExecutor: TaskExecutor
    override fun applyLog(context: StateMachineContext?, index: Int, commandBytes: ByteArray, firstLogIndex: Int) {
        taskExecutor.submit {
            if (context != null) {
                doApplyLog(context, index, commandBytes, firstLogIndex)
            }
        }
    }

    private fun doApplyLog(
        context: StateMachineContext,
        index: Int,
        commandBytes: ByteArray,
        firstLogIndex: Int
    ) {
        if (index <= lastApplied) {
            return
        }
        logger.debug("apply log {}", index)
        applyCommand(commandBytes)
        lastApplied = index
        if (shouldGenerateSnapshot(firstLogIndex, index)) {
            context.generateSnapshot(index)
        }
    }

    protected abstract fun applyCommand(commandBytes: ByteArray?)

    // run in node thread
    @Throws(IOException::class)
    override fun applySnapshot(snapshot: Snapshot) {
        logger.info("apply snapshot, last included index {}", snapshot.lastIncludedIndex)
        doApplySnapshot(snapshot.dataStream)
        lastApplied = snapshot.lastIncludedIndex
    }

    @Throws(IOException::class)
    protected abstract fun doApplySnapshot(input: InputStream?)
    override fun shutdown() {
        try {
            taskExecutor.shutdown()
        } catch (e: InterruptedException) {
            throw StateMachineException(e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractSingleThreadStateMachine::class.java)
    }

    init {
        taskExecutor = SingleThreadTaskExecutor("state-machine")
    }
}

