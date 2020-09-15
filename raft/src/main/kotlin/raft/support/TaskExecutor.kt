package raft.support

import com.google.common.util.concurrent.FutureCallback
import org.jetbrains.annotations.NotNull
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Future

/*
Todo use kotlinx coroutines actor instead of thread executor.
*/

interface TaskExecutor {
    @NotNull
    fun submit(task:Runnable): Future<*>

    @NotNull
    fun submit(task:Callable<*>): Future<*>

    fun submit(task: Runnable, callback: FutureCallback<*>)

    fun submit(task: Runnable, callbacks: Collection<FutureCallback<*>>)

    @Throws(InterruptedException::class)
    fun shutdown()
}