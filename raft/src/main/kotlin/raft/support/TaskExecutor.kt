package raft.support

interface TaskExecutor {
    // todo reorg to coroutine submit()
    suspend fun submit()

    suspend fun shutdown()
}