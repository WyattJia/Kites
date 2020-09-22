package raft.schedule

import java.util.concurrent.*
import javax.annotation.Nonnull

class NullScheduledFuture : ScheduledFuture<Any?> {
    override fun getDelay(@Nonnull unit: TimeUnit): Long {
        return 0
    }

    override fun compareTo(@Nonnull o: Delayed): Int {
        return 0
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return false
    }

    override fun isCancelled(): Boolean {
        return false
    }

    override fun isDone(): Boolean {
        return false
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): Any? {
        return null
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, @Nonnull unit: TimeUnit): Any? {
        return null
    }
}