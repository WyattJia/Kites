package raft.schedule

import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ElectionTimeout {

    private var scheduledFuture: ScheduledFuture<*>

    constructor(scheduledFuture: ScheduledFuture<*>) {
        this.scheduledFuture = scheduledFuture
    }

    fun cancel() {
        logger.debug("cancel election timeout")
        scheduledFuture.cancel(false)
    }

    override fun toString(): String {
        if (scheduledFuture.isCancelled) {
            return "ElectionTimeout(state=cancelled)"
        }
        return if (scheduledFuture.isDone) {
            "ElectionTimeout(state=done)"
        } else "ElectionTimeout{delay=" + scheduledFuture.getDelay(TimeUnit.MILLISECONDS) + "ms}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ElectionTimeout::class.java)
        val NONE = ElectionTimeout(NullScheduledFuture())
    }
}
