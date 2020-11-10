package raft.support

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FutureCallback
import java.util.concurrent.*
import java.util.function.Consumer


class SingleThreadTaskExecutor private constructor(threadFactory: ThreadFactory) : AbstractTaskExecutor() {
    private val executorService: ExecutorService

    constructor() : this(Executors.defaultThreadFactory()) {}
    constructor(name: String?) : this(ThreadFactory { r: Runnable? -> Thread(r, name) }) {}

    override fun submit(task: Runnable): Future<*> {
        Preconditions.checkNotNull(task)
        return executorService.submit(task)
    }

    override fun submit(task: Callable<*>): Future<*> {
        Preconditions.checkNotNull(task)
        return executorService.submit(task)
    }



    override fun submit(task: Runnable, callbacks: Collection<FutureCallback<*>>) {

        executorService.submit {
            try {
                task.run()
                callbacks.forEach(Consumer { c: FutureCallback<Any?> ->
                    c.onSuccess(
                        null
                    )
                })
            } catch (e: Exception) {
                callbacks.forEach(Consumer { c: FutureCallback<Any?> ->
                    c.onFailure(
                        e
                    )
                })
            }
        }

    }

    @Throws(InterruptedException::class)
    override fun shutdown() {
        executorService.shutdown()
        executorService.awaitTermination(1, TimeUnit.SECONDS)
    }

    init {
        executorService = Executors.newSingleThreadExecutor(threadFactory)
    }
}
