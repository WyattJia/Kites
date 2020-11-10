package kvstore.client

import raft.service.AddNodeCommand
import raft.service.Channel
import raft.service.ChannelException
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket


class SocketChannel(private val host: String, private val port: Int) : Channel {
    override fun send(payload: Any?): Any? {
        try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port))
                if (payload != null) {
                    this.write(socket.getOutputStream(), payload)
                }
                return read(socket.getInputStream())
            }
        } catch (e: IOException) {
            throw ChannelException("failed to send and receive", e)
        }
    }

    @Throws(IOException::class)
    private fun read(input: InputStream): Any? {
        val dataInput = DataInputStream(input)
        val messageType = dataInput.readInt()
        val payloadLength = dataInput.readInt()
        val payload = ByteArray(payloadLength)
        dataInput.readFully(payload)
        return when (messageType) {
            MessageConstants.MSG_TYPE_SUCCESS -> null
            MessageConstants.MSG_TYPE_FAILURE -> {
                val protoFailure: Protos.Failure = Protos.Failure.parseFrom(payload)
                throw ChannelException(
                    "error code " + protoFailure.getErrorCode().toString() + ", message " + protoFailure.getMessage()
                )
            }
            MessageConstants.MSG_TYPE_REDIRECT -> {
                val protoRedirect: Protos.Redirect = Protos.Redirect.parseFrom(payload)
                throw RedirectException(NodeId(protoRedirect.getLeaderId()))
            }
            MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE -> {
                val protoGetCommandResponse: GetCommandResponse = Protos.GetCommandResponse.parseFrom(payload)
                if (!protoGetCommandResponse.getFound()) null else protoGetCommandResponse.getValue().toByteArray()
            }
            else -> throw ChannelException("unexpected message type $messageType")
        }
    }

    @Throws(IOException::class)
    private fun write(output: OutputStream, payload: Any) {
        if (payload is GetCommand) {
            val protoGetCommand: GetCommand =
                Protos.GetCommand.newBuilder().setKey((payload as GetCommand).getKey()).build()
            this.write(output, MessageConstants.MSG_TYPE_GET_COMMAND, protoGetCommand)
        } else if (payload is SetCommand) {
            val setCommand: SetCommand = payload as SetCommand
            val protoSetCommand: SetCommand = Protos.SetCommand.newBuilder()
                .setKey(setCommand.getKey())
                .setValue(ByteString.copyFrom(setCommand.getValue())).build()
            this.write(output, MessageConstants.MSG_TYPE_SET_COMMAND, protoSetCommand)
        } else if (payload is AddNodeCommand) {
            val command: AddNodeCommand = payload as AddNodeCommand
            val protoAddServerCommand: AddNodeCommand =
                Protos.AddNodeCommand.newBuilder().setNodeId(command.getNodeId())
                    .setHost(command.getHost()).setPort(command.getPort()).build()
            this.write(output, MessageConstants.MSG_TYPE_ADD_SERVER_COMMAND, protoAddServerCommand)
        } else if (payload is RemoveNodeCommand) {
            val command: RemoveNodeCommand = payload as RemoveNodeCommand
            val protoRemoveServerCommand: RemoveNodeCommand =
                Protos.RemoveNodeCommand.newBuilder().setNodeId(command.getNodeId().getValue()).build()
            this.write(output, MessageConstants.MSG_TYPE_REMOVE_SERVER_COMMAND, protoRemoveServerCommand)
        }
    }

    @Throws(IOException::class)
    private fun write(output: OutputStream, messageType: Int, message: MessageLite) {
        val dataOutput = DataOutputStream(output)
        val messageBytes: ByteArray = message.toByteArray()
        dataOutput.writeInt(messageType)
        dataOutput.writeInt(messageBytes.size)
        dataOutput.write(messageBytes)
        dataOutput.flush()
    }

}

