package raft.node

import com.google.common.eventbus.EventBus
import raft.node.NodeGroup.NodeGroup
import raft.node.NodeId.NodeId
import raft.node.store.NodeStore
import raft.rpc.Connector
import raft.schedule.Scheduler
import raft.support.TaskExecutor


class NodeContext {
    lateinit var selfId: NodeId
    lateinit var group: NodeGroup
    lateinit var connector: Connector
    lateinit var scheduler: Scheduler
    lateinit var eventbus: EventBus
    lateinit var taskExecutor: TaskExecutor
    lateinit var store: NodeStore

    fun selfId(): NodeId {
        return selfId
    }

    @JvmName("setSelfId1")
    public fun setSelfId(selfId: NodeId) {
        this.selfId = selfId
    }

    fun store(): NodeStore? {
        return store
    }

    fun group():NodeGroup {
        return group
    }

}