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
    var store: NodeStore? = null
    var scheduler: Scheduler? = null
    var mode: NodeMode? = null
    var config: NodeConfig? = null
    var eventBus: EventBus? = null
    var taskExecutor: TaskExecutor? = null
    var groupConfigChangeTaskExecutor: TaskExecutor? = null

}


