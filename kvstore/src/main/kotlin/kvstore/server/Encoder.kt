package kvstore.server

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import kvstore.message.GetCommandResponse
import kvstore.message.Success
import raft.service.AddNodeCommand
import java.io.IOException


class Encoder : MessageToByteEncoder<Any>() {
    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext, msg: Any, out: ByteBuf) {
        if (msg is Success) {
            writeMessage(MessageConstants.MSG_TYPE_SUCCESS, Protos.Success.newBuilder().build(), out)
        } else if (msg is Failure) {
            val failure: Failure = msg as Failure
            val protoFailure: Protos.Failure =
                Protos.Failure.newBuilder().setErrorCode(failure.getErrorCode()).setMessage(failure.getMessage())
                    .build()
            writeMessage(MessageConstants.MSG_TYPE_FAILURE, protoFailure, out)
        } else if (msg is Redirect) {
            val redirect: Redirect = msg as Redirect
            val protoRedirect: Protos.Redirect =
                Protos.Redirect.newBuilder().setLeaderId(redirect.getLeaderId()).build()
            writeMessage(MessageConstants.MSG_TYPE_REDIRECT, protoRedirect, out)
        } else if (msg is AddNodeCommand) {
            val command: AddNodeCommand = msg as AddNodeCommand
            val protoCommand: AddNodeCommand = Protos.AddNodeCommand.newBuilder().setNodeId(command.getNodeId())
                .setHost(command.getHost()).setPort(command.getPort()).build()
            writeMessage(MessageConstants.MSG_TYPE_ADD_SERVER_COMMAND, protoCommand, out)
        } else if (msg is RemoveNodeCommand) {
            val command: RemoveNodeCommand = msg as RemoveNodeCommand
            val protoCommand: RemoveNodeCommand =
                Protos.RemoveNodeCommand.newBuilder().setNodeId(command.getNodeId().getValue()).build()
            writeMessage(MessageConstants.MSG_TYPE_REMOVE_SERVER_COMMAND, protoCommand, out)
        } else if (msg is GetCommand) {
            val command: GetCommand = msg as GetCommand
            val protoGetCommand: GetCommand = Protos.GetCommand.newBuilder().setKey(command.getKey()).build()
            writeMessage(MessageConstants.MSG_TYPE_GET_COMMAND, protoGetCommand, out)
        } else if (msg is GetCommandResponse) {
            val response: GetCommandResponse = msg as GetCommandResponse
            val value: ByteArray = response.getValue()
            val protoResponse: GetCommandResponse = Protos.GetCommandResponse.newBuilder()
                .setFound(response.isFound())
                .setValue(if (value != null) ByteString.copyFrom(value) else ByteString.EMPTY).build()
            writeMessage(MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE, protoResponse, out)
        } else if (msg is SetCommand) {
            val command: SetCommand = msg as SetCommand
            val protoSetCommand: SetCommand = Protos.SetCommand.newBuilder()
                .setKey(command.getKey())
                .setValue(ByteString.copyFrom(command.getValue()))
                .build()
            writeMessage(MessageConstants.MSG_TYPE_SET_COMMAND, protoSetCommand, out)
        }
    }

    @Throws(IOException::class)
    private fun writeMessage(messageType: Int, message: MessageLite, out: ByteBuf) {
        out.writeInt(messageType)
        val bytes: ByteArray = message.toByteArray()
        out.writeInt(bytes.size)
        out.writeBytes(bytes)
    }
}

