package raft.node.task

import org.slf4j.LoggerFactory

abstract class AbstractGroupConfigChangeTask(context: GroupConfigChangeTaskContext) : GroupConfigChangeTask {
    protected enum class State {
        START, GROUP_CONFIG_APPENDED, GROUP_CONFIG_COMMITTED, TIMEOUT
    }

    protected val context: GroupConfigChangeTaskContext
    protected var state = State.START
    @Synchronized
    @Throws(Exception::class)
    override fun call(): GroupConfigChangeTaskResult {
        logger.debug("task start")
        this.state = State.START
        appendGroupConfig()
        this.state = State.GROUP_CONFIG_APPENDED
        // todo use coroutine
        java.lang.Object().wait()
        logger.debug("task done")
        context.done()
        return mapResult(state)
    }

    private fun mapResult(state: State): GroupConfigChangeTaskResult {
        return when (state) {
            State.GROUP_CONFIG_COMMITTED -> GroupConfigChangeTaskResult.OK
            else -> GroupConfigChangeTaskResult.TIMEOUT
        }
    }

    protected abstract fun appendGroupConfig()
    @Synchronized
    override fun onLogCommitted() {
        check(state == State.GROUP_CONFIG_APPENDED) { "log committed before log appended" }
        this.state = State.GROUP_CONFIG_COMMITTED

        // todo use coroutines
        Object().notify()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractGroupConfigChangeTask::class.java)
    }

    init {
        this.context = context
    }
}