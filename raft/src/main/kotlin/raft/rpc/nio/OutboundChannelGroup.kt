package raft.rpc.nio

import com.google.common.eventbus.EventBus
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.slf4j.LoggerFactory
import raft.node.Address
import raft.node.NodeId
import raft.rpc.ChannelConnectException
import java.net.ConnectException
import java.util.concurrent.*
import java.util.function.BiConsumer
import javax.annotation.concurrent.ThreadSafe


@ThreadSafe
class OutboundChannelGroup(
    private val workerGroup: EventLoopGroup,
    private val eventBus: EventBus,
    selfNodeId: NodeId,
    logReplicationInterval: Int
) {
    private val selfNodeId: NodeId = selfNodeId
    private val connectTimeoutMillis: Int = logReplicationInterval / 2
    // thread safe
    private val channelMap: ConcurrentMap<NodeId, Future<NioChannel>> = ConcurrentHashMap<NodeId, Future<NioChannel>>()
    fun getOrConnect(nodeId: NodeId, address: Address): NioChannel {
        var future = channelMap[nodeId]
        if (future == null) {
            val newFuture = FutureTask {
                connect(
                    nodeId,
                    address
                )
            }
            future = channelMap.putIfAbsent(nodeId, newFuture)
            if (future == null) {
                future = newFuture
                newFuture.run()
            }
        }
        try {
            return future.get()
        } catch (e: Exception) {
            channelMap.remove(nodeId)
            if (e is ExecutionException) {
                val cause = e.cause
                if (cause is ConnectException) {
                    throw ChannelConnectException(
                        "failed to get channel to node " + nodeId +
                                ", cause " + cause.message, cause
                    )
                }
            }
            throw ChannelException("failed to get channel to node $nodeId", e)
        }
    }

    @Throws(InterruptedException::class)
    private fun connect(nodeId: NodeId, address: Address): NioChannel {
        val bootstrap = Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
            .handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addLast(Decoder())
                    pipeline.addLast(Encoder())
                    pipeline.addLast(ToRemoteHandler(eventBus, nodeId, selfNodeId))
                }
            })
        val future: ChannelFuture = bootstrap.connect(address.host, address.port).sync()
        if (!future.isSuccess) {
            throw ChannelException("failed to connect", future.cause())
        }
        logger.debug("channel OUTBOUND-{} connected", nodeId)
        val nettyChannel = future.channel()
        nettyChannel.closeFuture().addListener(ChannelFutureListener { cf: ChannelFuture? ->
            logger.debug("channel OUTBOUND-{} disconnected", nodeId)
            channelMap.remove(nodeId)
        })
        return NioChannel(nettyChannel)
    }

    fun closeAll() {
        logger.debug("close all outbound channels")
        channelMap.forEach(BiConsumer<NodeId, Future<NioChannel>> { nodeId: NodeId?, nioChannelFuture: Future<NioChannel> ->
            try {
                nioChannelFuture.get().close()
            } catch (e: Exception) {
                logger.warn("failed to close", e)
            }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OutboundChannelGroup::class.java)
    }

}

