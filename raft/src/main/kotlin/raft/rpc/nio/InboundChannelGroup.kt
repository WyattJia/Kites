package raft.rpc.nio

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import org.slf4j.LoggerFactory
import raft.node.NodeId
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.concurrent.ThreadSafe


@ThreadSafe
class InboundChannelGroup {
    private val channels: MutableList<NioChannel> = CopyOnWriteArrayList<NioChannel>()
    fun add(remoteId: NodeId?, channel: NioChannel) {
        logger.debug("channel INBOUND-{} connected", remoteId)
        channel.nettyChannel.closeFuture().addListener(ChannelFutureListener { future: ChannelFuture? ->
            logger.debug("channel INBOUND-{} disconnected", remoteId)
            remove(channel)
        })
    }

    private fun remove(channel: NioChannel) {
        channels.remove(channel)
    }

    fun closeAll() {
        logger.debug("close all inbound channels")
        for (channel in channels) {
            channel.close()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InboundChannelGroup::class.java)
    }
}

