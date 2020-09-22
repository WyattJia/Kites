package raft.schedule

import org.slf4j.LoggerFactory

class NullScheduler : Scheduler {

    override fun scheduleLogReplicationTask(task: Runnable): LogReplicationTask {
        logger.debug("schedule log replication task")
        return LogReplicationTask.NONE
    }

    override fun scheduleElectionTimeout(task: Runnable): ElectionTimeout {
        logger.debug("schedule election timeout")
        return ElectionTimeout.NONE
    }

    @Throws(InterruptedException::class)
    override fun stop() {
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NullScheduler::class.java)
    }
}