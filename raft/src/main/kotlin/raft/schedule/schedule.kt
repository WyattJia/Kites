package raft.schedule

interface Scheduler {
    // todo: Check kotlin's runnable type
    fun scheduleLogReplicationTask(task: java.lang.Runnable): LogReplicationTask

    fun scheduleElectionTimeout(task: java.lang.Runnable): ElectionTimeout

    @Throws(InterruptedException::class)
    fun stop()
}
