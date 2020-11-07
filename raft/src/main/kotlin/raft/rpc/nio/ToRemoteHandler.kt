package raft.rpc.nio

import com.google.common.eventbus.EventBus
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory
import raft.node.NodeId

class ToRemoteHandler(eventBus: EventBus?, remoteId: NodeId, selfNodeId: NodeId) :
    AbstractHandler(eventBus) {
    private val selfNodeId: NodeId
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.write(selfNodeId)
        channel = NioChannel(ctx.channel())
    }

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        logger.debug("receive {} from {}", msg, remoteId)
        super.channelRead(ctx, msg)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ToRemoteHandler::class.java)
    }

    init {
        this.remoteId = remoteId
        this.selfNodeId = selfNodeId
    }
}

