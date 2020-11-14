package raft.node.task

import java.util.concurrent.TimeoutException


/**
 * Reference for group config change task.
 */
interface GroupConfigChangeTaskReference {
    /**
     * Wait for result forever.
     *
     * @return result
     * @throws InterruptedException if interrupted
     */
    @get:Throws(InterruptedException::class)
    val result: GroupConfigChangeTaskResult?

    /**
     * Wait for result in specified timeout.
     *
     * @param timeout timeout
     * @return result
     * @throws InterruptedException if interrupted
     * @throws TimeoutException if timeout
     */
    @Throws(InterruptedException::class, TimeoutException::class)
    fun getResult(timeout: Long): GroupConfigChangeTaskResult?

    /**
     * Cancel task.
     */
    fun cancel()
}

