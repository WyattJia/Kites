package raft.schedule

import java.util.concurrent.ScheduledFuture

abstract class ElectionTimeout {
    // todo find kotlin's logger best practice
//    val logger: Logger = LoggerFactory.getLogger(javaClass<ElectionTimeout>())
    val None:ElectionTimeout = ElectionTimeout(NullSchedulerFuture())

    // kotlin scheduledFuture usage
    val scheduledFuture: ScheduledFuture<Any>

    fun cancel(){
        this.scheduledFuture.cancel(false)
    }

}