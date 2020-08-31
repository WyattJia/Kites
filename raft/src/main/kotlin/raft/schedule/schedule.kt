package raft.schedule

public interface Scheduler {
    // todo: Check kotlin's runnable type
    fun scheduleLogReplicationTask(task: java.lang.Runnable!!): LogReplicationTask!!

    fun scheduleElectionTimeout(task: java.lang.Runnable!!): ElectionTimeout

    fun stop() throw InterruptedException

}
