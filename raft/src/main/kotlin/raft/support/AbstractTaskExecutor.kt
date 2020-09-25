package raft.support

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FutureCallback
import java.util.*

abstract class AbstractTaskExecutor : TaskExecutor {
    override fun submit(task: Runnable, callback: FutureCallback<*>) {
        Preconditions.checkNotNull(task)
        Preconditions.checkNotNull(callback)
        submit(task, Collections.singletonList(callback))
    }
}