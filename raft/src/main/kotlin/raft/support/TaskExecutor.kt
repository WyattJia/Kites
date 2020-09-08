package raft.support

interface TaskExecutor {
    // todo reorg to coroutine submit()
    fun submit()

    fun shutdown()
}