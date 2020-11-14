package raft.node

import com.google.common.eventbus.EventBus
import raft.log.Log
import raft.node.config.NodeConfig
import raft.node.store.NodeStore
import raft.rpc.Connector
import raft.schedule.Scheduler
import raft.support.TaskExecutor

/**
 * Node context.
 *
 *
 * Node context should not change after initialization. e.g [NodeBuilder].
 *
 */
class NodeContext {
    var selfId: NodeId? = null
    var group: NodeGroup? = null
    var log: Log? = null
    var connector: Connector? = null
    private var store: NodeStore? = null
    private var scheduler: Scheduler? = null
    private var mode: NodeMode? = null
    var config: NodeConfig? = null
    var eventBus: EventBus? = null
    private var taskExecutor: TaskExecutor? = null
    private var groupConfigChangeTaskExecutor: TaskExecutor? = null
    fun selfId(): NodeId? {
        return selfId
    }

    fun setSelfId(selfId: NodeId?) {
        this.selfId = selfId
    }

    fun group(): NodeGroup? {
        return group
    }

    fun setGroup(group: NodeGroup?) {
        this.group = group
    }

    fun log(): Log? {
        return log
    }

    fun setLog(log: Log?) {
        this.log = log
    }

    fun connector(): Connector? {
        return connector
    }

    fun setConnector(connector: Connector?) {
        this.connector = connector
    }

    fun store(): NodeStore? {
        return store
    }

    fun setStore(store: NodeStore?) {
        this.store = store
    }

    fun scheduler(): Scheduler? {
        return scheduler
    }

    fun setScheduler(scheduler: Scheduler?) {
        this.scheduler = scheduler
    }

    fun mode(): NodeMode? {
        return mode
    }

    fun setMode(mode: NodeMode?) {
        this.mode = mode
    }

    fun config(): NodeConfig? {
        return config
    }

    fun setConfig(config: NodeConfig?) {
        this.config = config
    }

    fun eventBus(): EventBus? {
        return eventBus
    }

    fun setEventBus(eventBus: EventBus?) {
        this.eventBus = eventBus
    }

    fun taskExecutor(): TaskExecutor? {
        return taskExecutor
    }

    fun setTaskExecutor(taskExecutor: TaskExecutor?) {
        this.taskExecutor = taskExecutor
    }

    fun groupConfigChangeTaskExecutor(): TaskExecutor? {
        return groupConfigChangeTaskExecutor
    }

    fun setGroupConfigChangeTaskExecutor(groupConfigChangeTaskExecutor: TaskExecutor?) {
        this.groupConfigChangeTaskExecutor = groupConfigChangeTaskExecutor
    }
}


