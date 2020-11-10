package kvstore.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import kvstore.message.CommandRequest
import kvstore.message.GetCommand
import kvstore.message.SetCommand
import raft.service.AddNodeCommand
import raft.service.RemoveNodeCommand


class ServiceHandler(private val service: Service) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is AddNodeCommand) {
            service.addNode(CommandRequest(msg as AddNodeCommand, ctx.channel()))
        } else if (msg is RemoveNodeCommand) {
            service.removeNode(CommandRequest(msg as RemoveNodeCommand, ctx.channel()))
        } else if (msg is GetCommand) {
            service[CommandRequest(msg as GetCommand, ctx.channel())]
        } else if (msg is SetCommand) {
            service.set(CommandRequest(msg as SetCommand, ctx.channel()))
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}

