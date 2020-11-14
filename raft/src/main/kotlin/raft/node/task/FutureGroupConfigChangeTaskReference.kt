package raft.node.task

import org.slf4j.LoggerFactory
import raft.node.task.FutureGroupConfigChangeTaskReference
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class FutureGroupConfigChangeTaskReference(future: Future<GroupConfigChangeTaskResult>) :
    GroupConfigChangeTaskReference {
    private val future: Future<GroupConfigChangeTaskResult> = future

    @get:Throws(InterruptedException::class)
    override val result: GroupConfigChangeTaskResult?
        get() = try {
            future.get()
        } catch (e: ExecutionException) {
            logger.warn("task execution failed", e)
            GroupConfigChangeTaskResult.ERROR
        }

    @Throws(InterruptedException::class, TimeoutException::class)
    override fun getResult(timeout: Long): GroupConfigChangeTaskResult {
        return try {
            future[timeout, TimeUnit.MILLISECONDS]
        } catch (e: ExecutionException) {
            logger.warn("task execution failed", e)
            GroupConfigChangeTaskResult.ERROR
        }
    }

    override fun cancel() {
        future.cancel(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FutureGroupConfigChangeTaskReference::class.java)
    }

}