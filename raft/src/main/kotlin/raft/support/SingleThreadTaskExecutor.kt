package raft.support

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FutureCallback
import java.util.concurrent.*


class SingleThreadTaskExecutor(private var executorService: ExecutorService): AbstractTaskExecutor() {

    constructor(threadFactory: ThreadFactory) {
        this.executorService = Executors.newSingleThreadExecutor(threadFactory)
    }

    override fun submit(task: Runnable): Future<*> {
        Preconditions.checkNotNull(task);
        return executorService.submit(task);

    }

    override fun submit(task: Callable<*>): Future<*> {
        Preconditions.checkNotNull(task);
        return executorService.submit(task);
    }

    override fun submit(task: Runnable, callbacks: Collection<FutureCallback<*>>) {
        Preconditions.checkNotNull<Any>(task)
        Preconditions.checkNotNull<Any>(callbacks)
        executorService.submit {
            try {
                task.run()
                callbacks.forEach { c -> c.onSuccess(null) }
            } catch (e: Exception) {
                callbacks.forEach { c -> c.onFailure(e) }
            }
        }
    }

    override fun shutdown() {
        executorService.shutdown()
        executorService.awaitTermination(1, TimeUnit.SECONDS)
    }
}