package raft.support

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FutureCallback
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask


class DirectTaskExecutor(private var throwWhenFailed: Boolean) : AbstractTaskExecutor() {

    init {
        this.throwWhenFailed = false
    }

    override fun submit(task: Runnable): Future<*> {
        Preconditions.checkNotNull(task)
        val futureTask: FutureTask<*> = FutureTask<Any?>(task, null)
        futureTask.run()
        return futureTask
    }

    override fun submit(task: Callable<*>): Future<*> {
        Preconditions.checkNotNull(task)
        val futureTask = FutureTask(task)
        futureTask.run()
        return futureTask
    }

    override fun submit(task: Runnable, callbacks: Collection<FutureCallback<*>>) {
        Preconditions.checkNotNull(task)
        Preconditions.checkNotNull(callbacks)
        try {
            task.run()
            callbacks.forEach { c -> c.onSuccess(null) }
        } catch (t: Throwable) {
            callbacks.forEach { c -> c.onFailure(t) }
            if (throwWhenFailed) {
                throw t
            }
        }
    }

    @Throws(InterruptedException::class)
    override fun shutdown() {
    }


}