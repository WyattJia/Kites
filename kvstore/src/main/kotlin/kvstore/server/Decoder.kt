package kvstore.server

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder


class Decoder : ByteToMessageDecoder() {
    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
        if (`in`.readableBytes() < 8) return
        `in`.markReaderIndex()
        val messageType = `in`.readInt()
        val payloadLength = `in`.readInt()
        if (`in`.readableBytes() < payloadLength) {
            `in`.resetReaderIndex()
            return
        }
        val payload = ByteArray(payloadLength)
        `in`.readBytes(payload)
        when (messageType) {
            MessageConstants.MSG_TYPE_SUCCESS -> out.add(Success.INSTANCE)
            MessageConstants.MSG_TYPE_FAILURE -> {
                val protoFailure: Protos.Failure = Protos.Failure.parseFrom(payload)
                out.add(Failure(protoFailure.getErrorCode(), protoFailure.getMessage()))
            }
            MessageConstants.MSG_TYPE_REDIRECT -> {
                val protoRedirect: Protos.Redirect = Protos.Redirect.parseFrom(payload)
                out.add(Redirect(protoRedirect.getLeaderId()))
            }
            MessageConstants.MSG_TYPE_ADD_SERVER_COMMAND -> {
                val protoAddServerCommand: AddNodeCommand = Protos.AddNodeCommand.parseFrom(payload)
                out.add(
                    AddNodeCommand(
                        protoAddServerCommand.getNodeId(),
                        protoAddServerCommand.getHost(),
                        protoAddServerCommand.getPort()
                    )
                )
            }
            MessageConstants.MSG_TYPE_REMOVE_SERVER_COMMAND -> {
                val protoRemoveServerCommand: RemoveNodeCommand = Protos.RemoveNodeCommand.parseFrom(payload)
                out.add(RemoveNodeCommand(protoRemoveServerCommand.getNodeId()))
            }
            MessageConstants.MSG_TYPE_GET_COMMAND -> {
                val protoGetCommand: GetCommand = Protos.GetCommand.parseFrom(payload)
                out.add(GetCommand(protoGetCommand.getKey()))
            }
            MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE -> {
                val protoGetCommandResponse: GetCommandResponse = Protos.GetCommandResponse.parseFrom(payload)
                out.add(
                    GetCommandResponse(
                        protoGetCommandResponse.getFound(),
                        protoGetCommandResponse.getValue().toByteArray()
                    )
                )
            }
            MessageConstants.MSG_TYPE_SET_COMMAND -> {
                val protoSetCommand: SetCommand = Protos.SetCommand.parseFrom(payload)
                out.add(SetCommand(protoSetCommand.getKey(), protoSetCommand.getValue().toByteArray()))
            }
            else -> throw IllegalStateException("unexpected message type $messageType")
        }
    }
}

