package raft.node

import com.google.common.eventbus.EventBus
import io.netty.channel.nio.NioEventLoopGroup
import raft.log.FileLog
import raft.log.Log
import raft.log.MemoryLog
import raft.node.config.NodeConfig
import raft.node.store.FileNodeStore
import raft.node.store.MemoryNodeStore
import raft.node.store.NodeStore
import raft.rpc.Connector
import raft.rpc.nio.NioConnector
import raft.schedule.Scheduler
import raft.support.TaskExecutor
import java.io.File


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
     * Node configuration.
     */
    private var config: NodeConfig = NodeConfig()

    /**
     * Starts as standby or not.
     */
    private var standby = false

    /**
     * Log.
     * If data directory specified, [FileLog] will be created.
     * Default to [MemoryLog].
     */
    private var log: Log? = null

    /**
     * Store for current term and last node id voted for.
     * If data directory specified, [FileNodeStore] will be created.
     * Default to [MemoryNodeStore].
     */
    private var store: NodeStore? = null

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
     * Task executor for group config change task, INTERNAL.
     */
    private var groupConfigChangeTaskExecutor: TaskExecutor? = null

    /**
     * Event loop group for worker.
     * If specified, reuse. otherwise create one.
     */
    private var workerNioEventLoopGroup: NioEventLoopGroup? = null

    // TODO add doc
    constructor(endpoint: NodeEndpoint) : this(listOf(endpoint), endpoint.id) {}

    // TODO add doc
    constructor(endpoints: Collection<NodeEndpoint>, selfId: NodeId) {
        group = NodeGroup(endpoints, selfId)
        this.selfId = selfId
        eventBus = EventBus(selfId.getValue())
    }

    /**
     * Create.
     *
     * @param selfId self id
     * @param group  group
     */
    @Deprecated("")
    constructor(selfId: NodeId, group: NodeGroup) {
        this.selfId = selfId
        this.group = group
        eventBus = EventBus(selfId.getValue())
    }

    /**
     * Set standby.
     *
     * @param standby standby
     * @return this
     */
    fun setStandby(standby: Boolean): NodeBuilder {
        this.standby = standby
        return this
    }

    /**
     * Set configuration.
     *
     * @param config config
     * @return this
     */
    fun setConfig(config: NodeConfig): NodeBuilder {
        this.config = config
        return this
    }

    /**
     * Set connector.
     *
     * @param connector connector
     * @return this
     */
    fun setConnector(connector: Connector): NodeBuilder {
        this.connector = connector
        return this
    }

    /**
     * Set event loop for worker.
     * If specified, it's caller's responsibility to close worker event loop.
     *
     * @param workerNioEventLoopGroup worker event loop
     * @return this
     */
    fun setWorkerNioEventLoopGroup(workerNioEventLoopGroup: NioEventLoopGroup): NodeBuilder {
        this.workerNioEventLoopGroup = workerNioEventLoopGroup
        return this
    }

    /**
     * Set scheduler.
     *
     * @param scheduler scheduler
     * @return this
     */
    fun setScheduler(scheduler: Scheduler): NodeBuilder {
        this.scheduler = scheduler
        return this
    }

    /**
     * Set task executor.
     *
     * @param taskExecutor task executor
     * @return this
     */
    fun setTaskExecutor(taskExecutor: TaskExecutor): NodeBuilder {
        this.taskExecutor = taskExecutor
        return this
    }

    /**
     * Set group config change task executor.
     *
     * @param groupConfigChangeTaskExecutor group config change task executor
     * @return this
     */
    fun setGroupConfigChangeTaskExecutor(groupConfigChangeTaskExecutor: TaskExecutor): NodeBuilder {
        this.groupConfigChangeTaskExecutor = groupConfigChangeTaskExecutor
        return this
    }

    /**
     * Set store.
     *
     * @param store store
     * @return this
     */
    fun setStore(store: NodeStore): NodeBuilder {
        this.store = store
        return this
    }

    /**
     * Set data directory.
     *
     * @param dataDirPath data directory
     * @return this
     */
    fun setDataDir(dataDirPath: String?): NodeBuilder {
        if (dataDirPath == null || dataDirPath.isEmpty()) {
            return this
        }
        val dataDir = File(dataDirPath)
        require(!(!dataDir.isDirectory || !dataDir.exists())) { "[$dataDirPath] not a directory, or not exists" }
        log = FileLog(dataDir)
        store = FileNodeStore(File(dataDir, FileNodeStore.FILE_NAME))
        return this
    }

    /**
     * Build node.
     *
     * @return node
     */
    fun build(): Node {
        return NodeImpl(buildContext())
    }

    /**
     * Build context for node.
     *
     * @return node context
     */
    private fun buildContext(): NodeContext {
        val context = NodeContext()
        context.group = group
        context.mode = evaluateMode()
        context.log = (if (log != null) log else MemoryLog())
        context.store = store
        context.selfId = selfId
        context.config = config
        context.eventBus = eventBus
        context.scheduler = scheduler
        context.connector = (if (connector != null) connector else createNioConnector())
        context.taskExecutor =  taskExecutor
        // TODO share monitor
        context.groupConfigChangeTaskExecutor = groupConfigChangeTaskExecutor
        return context
    }

    /**
     * Create nio connector.
     *
     * @return nio connector
     */
    private fun createNioConnector(): NioConnector {
        val port: Int = group.findSelf().endpoint.address.port
        return if (workerNioEventLoopGroup != null) {
            NioConnector(workerNioEventLoopGroup!!, selfId, eventBus, port, config.logReplicationInterval)
        } else NioConnector(
            NioEventLoopGroup(config.nioWorkerThreads), false,
            selfId, eventBus, port, config.logReplicationInterval)
    }

    /**
     * Evaluate mode.
     *
     * @return mode
     * @see NodeGroup.isStandalone
     */
    private fun evaluateMode(): NodeMode {
        if (standby) {
            return NodeMode.STANDBY
        }
        return if (group.isStandalone()) {
            NodeMode.STANDALONE
        } else NodeMode.GROUP_MEMBER
    }
}


