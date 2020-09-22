package raft.schedule

import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class LogReplicationTask(private val scheduledFuture: ScheduledFuture<*>) {
    fun cancel() {
        logger.debug("cancel log replication task")
        scheduledFuture.cancel(false)
    }

    override fun toString(): String {
        return "LogReplicationTask{delay=" + scheduledFuture.getDelay(TimeUnit.MILLISECONDS) + "}"
    }

    companion object {
        val NONE = LogReplicationTask(NullScheduledFuture())
        private val logger = LoggerFactory.getLogger(LogReplicationTask::class.java)
    }
}