package raft.rpc.nio

import com.google.common.eventbus.EventBus
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory
import raft.node.NodeId


class FromRemoteHandler(private val channelGroup: InboundChannelGroup?, eventBus: EventBus ) :
    AbstractHandler(eventBus) {

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is NodeId) {
            remoteId = msg as NodeId
            val nioChannel = NioChannel(ctx.channel())
            channel = nioChannel
            if (channelGroup != null) {
                channelGroup.add(remoteId, nioChannel)
            }
            return
        }
        logger.debug("receive {} from {}", msg, remoteId)
        super.channelRead(ctx, msg)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FromRemoteHandler::class.java)
    }
}


