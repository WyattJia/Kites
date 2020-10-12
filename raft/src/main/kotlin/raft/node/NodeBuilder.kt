package raft.node

import com.google.common.base.Preconditions
import com.google.common.eventbus.EventBus
import raft.node.NodeEndpoint.NodeEndpoint
import raft.node.NodeGroup.NodeGroup
import raft.node.store.NodeStore
import raft.rpc.Connector
import raft.schedule.Scheduler
import raft.support.TaskExecutor
import javax.annotation.Nonnull


/**
 * Node builder.
 */
class NodeBuilder {
    /**
     * Group.
     */
    private val group: NodeGroup

    /**
     * Self id.
     */
    private val selfId: NodeId

    /**
     * Event bus, INTERNAL.
     */
    private val eventBus: EventBus



    /**
     * Scheduler, INTERNAL.
     */
    private var scheduler: Scheduler? = null

    /**
     * Connector, component to communicate between nodes, INTERNAL.
     */
    private var connector: Connector? = null

    /**
     * Task executor for node, INTERNAL.
     */
    private var taskExecutor: TaskExecutor? = null

    /**
     * Store for current term and last node id voted for.
     * If data directory specified, [FileNodeStore] will be created.
     * Default to [MemoryNodeStore].
     */
    private var store: NodeStore? = null

    /*
    * 单节点构造函数
    */
    constructor(@Nonnull endpoint: NodeEndpoint) : this(listOf(endpoint), endpoint.id) {}

    /*
    多节点构造函数
     */
    constructor(@Nonnull endpoints: Collection<NodeEndpoint>, @Nonnull selfId: NodeId) {
        group = NodeGroup(endpoints, selfId)
        this.selfId = selfId
        eventBus = EventBus(selfId.getValue())
    }

    /**
     * Set connector.
     *
     * @param connector connector
     * @return this
     */
    fun setConnector(@Nonnull connector: Connector): NodeBuilder {
        Preconditions.checkNotNull<Any>(connector)
        this.connector = connector
        return this
    }



    /**
     * Set scheduler.
     *
     * @param scheduler scheduler
     * @return this
     */
    fun setScheduler(@Nonnull scheduler: Scheduler): NodeBuilder {
        Preconditions.checkNotNull<Any>(scheduler)
        this.scheduler = scheduler
        return this
    }

    /**
     * Set task executor.
     *
     * @param taskExecutor task executor
     * @return this
     */
    fun setTaskExecutor(@Nonnull taskExecutor: TaskExecutor): NodeBuilder {
        Preconditions.checkNotNull<Any>(taskExecutor)
        this.taskExecutor = taskExecutor
        return this
    }

    /**
     * Build node.
     *
     * @return node
     */
    @Nonnull
    fun build(): Node {
        return NodeImpl(buildContext())
    }

    /**
     * Build context for node.
     *
     * @return node context
     */
    @Nonnull
    private fun buildContext(): NodeContext {
        val context = NodeContext()
        context.group = group
        context.selfId = selfId
        context.eventbus = eventBus
        context.scheduler = scheduler!! // todo lambda {   != null ? scheduler: DefaultScheduler(config)   }
        context.connector = connector!!
        context.taskExecutor = taskExecutor!! //todo lambda {   != null ?  new SingleThreadTaskExecutor   }

        return context
    }

    /**
     * Set store.
     *
     * @param store store
     * @return this
     */
    fun setStore(store: NodeStore): NodeBuilder {
        Preconditions.checkNotNull<Any>(store)
        this.store = store
        return this
    }
}

