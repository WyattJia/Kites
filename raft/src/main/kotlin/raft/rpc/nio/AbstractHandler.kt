package raft.rpc.nio

import com.google.common.eventbus.EventBus
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.slf4j.LoggerFactory
import raft.node.NodeId
import raft.rpc.Channel
import raft.rpc.message.*


abstract class AbstractHandler(protected val eventBus: EventBus?) : ChannelDuplexHandler() {
    var remoteId: NodeId? = null
    protected var channel: Channel? = null
    private var lastAppendEntriesRpc: AppendEntriesRpc? = null
    private var lastInstallSnapshotRpc: InstallSnapshotRpc? = null

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        assert(remoteId != null)
        assert(channel != null)
        if (msg is RequestVoteRpc) {
            val rpc: RequestVoteRpc = msg as RequestVoteRpc
            eventBus!!.post(RequestVoteRpcMessage(rpc, remoteId, channel))
        } else if (msg is RequestVoteResult) {
            eventBus!!.post(msg)
        } else if (msg is AppendEntriesRpc) {
            val rpc: AppendEntriesRpc = msg as AppendEntriesRpc
            eventBus!!.post(remoteId?.let { AppendEntriesRpcMessage(rpc, it, channel) })
        } else if (msg is AppendEntriesResult) {
            val result: AppendEntriesResult = msg as AppendEntriesResult
            if (lastAppendEntriesRpc == null) {
                logger.warn("no last append entries rpc")
            } else {
                if (result.rpcMessageId != lastAppendEntriesRpc!!.messageId) {
                    logger.warn(
                        "incorrect append entries rpc message id {}, expected {}",
                        result.rpcMessageId,
                        lastAppendEntriesRpc!!.messageId
                    )
                } else {
                    eventBus!!.post(AppendEntriesResultMessage(result, remoteId!!, lastAppendEntriesRpc!!))
                    lastAppendEntriesRpc = null
                }
            }
        } else if (msg is InstallSnapshotRpc) {
            val rpc: InstallSnapshotRpc = msg as InstallSnapshotRpc
            eventBus!!.post(InstallSnapshotRpcMessage(rpc, remoteId, channel))
        } else if (msg is InstallSnapshotResult) {
            val result: InstallSnapshotResult = msg as InstallSnapshotResult
            assert(lastInstallSnapshotRpc != null)
            eventBus!!.post(InstallSnapshotResultMessage(result, remoteId!!, lastInstallSnapshotRpc!!))
            lastInstallSnapshotRpc = null
        }
    }

    @Throws(Exception::class)
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (msg is AppendEntriesRpc) {
            lastAppendEntriesRpc = msg as AppendEntriesRpc
        } else if (msg is InstallSnapshotRpc) {
            lastInstallSnapshotRpc = msg as InstallSnapshotRpc
        }
        super.write(ctx, msg, promise)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn(cause.message, cause)
        ctx.close()
    }


    companion object {
        private val logger = LoggerFactory.getLogger(OutboundChannelGroup::class.java)
    }
}
