package raft.node.task

import org.slf4j.LoggerFactory
import raft.log.entry.AddNodeEntry
import raft.log.entry.GroupConfigEntry
import raft.log.entry.RemoveNodeEntry
import java.util.concurrent.TimeoutException
import javax.annotation.concurrent.Immutable

@Immutable
class GroupConfigChangeTaskHolder constructor(
    private val task: GroupConfigChangeTask = GroupConfigChangeTask.NONE,
    reference: GroupConfigChangeTaskReference = FixedResultGroupConfigTaskReference(
        GroupConfigChangeTaskResult.OK
    )
) {
    private val reference: GroupConfigChangeTaskReference
    @Throws(TimeoutException::class, InterruptedException::class)
    fun awaitDone(timeout: Long) {
        if (timeout == 0L) {
            reference.result
        } else {
            reference.getResult(timeout)
        }
    }

    fun cancel() {
        reference.cancel()
    }

    val isEmpty: Boolean
        get() = task === GroupConfigChangeTask.NONE

    fun onLogCommitted(entry: GroupConfigEntry): Boolean {
        if (isEmpty) {
            return false
        }
        logger.debug("log {} committed, current task {}", entry, task)
        if (entry is AddNodeEntry && task is AddNodeTask
            && task.isTargetNode(entry.getNewNodeEndpoint().id)
        ) {
            task.onLogCommitted()
            return true
        }
        if (entry is RemoveNodeEntry && task is RemoveNodeTask
            && task.isTargetNode(entry.getNodeToRemove())
        ) {
            task.onLogCommitted()
            return true
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupConfigChangeTaskHolder::class.java)
    }

    init {
        this.reference = reference
    }
}