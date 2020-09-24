package raft.node

import com.google.common.eventbus.EventBus
import raft.log.Log
import raft.node.NodeGroup.NodeGroup
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
    lateinit var eventbus: EventBus
    lateinit var selfId: NodeId
    lateinit var group: NodeGroup
    lateinit var log: Log
    lateinit var connector: Connector
    lateinit var store: NodeStore
    lateinit var scheduler: Scheduler
    lateinit var mode: NodeMode
    lateinit var config: NodeConfig
    lateinit var taskExecutor: TaskExecutor
    lateinit var groupConfigChangeTaskExecutor: TaskExecutor
}
