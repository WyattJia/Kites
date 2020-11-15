package raft.rpc.nio

//import com.google.protobuf.MessageLite
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder


class Encoder : MessageToByteEncoder<Any>() {
    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext, msg: Any, out: ByteBuf) {
        TODO("Not yet implemented")

//        if (msg is NodeId) {
//            this.writeMessage(out, MessageConstants.MSG_TYPE_NODE_ID, (msg as NodeId).getValue().getBytes())
//        } else if (msg is RequestVoteRpc) {
//            val rpc: RequestVoteRpc = msg as RequestVoteRpc
//            val protoRpc: RequestVoteRpc = Protos.RequestVoteRpc.newBuilder()
//                .setTerm(rpc.getTerm())
//                .setCandidateId(rpc.getCandidateId().getValue())
//                .setLastLogIndex(rpc.getLastLogIndex())
//                .setLastLogTerm(rpc.getLastLogTerm())
//                .build()
//            this.writeMessage(out, MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC, protoRpc)
//        } else if (msg is RequestVoteResult) {
//            val result: RequestVoteResult = msg as RequestVoteResult
//            val protoResult: RequestVoteResult = Protos.RequestVoteResult.newBuilder()
//                .setTerm(result.getTerm())
//                .setVoteGranted(result.isVoteGranted())
//                .build()
//            this.writeMessage(out, MessageConstants.MSG_TYPE_REQUEST_VOTE_RESULT, protoResult)
//        } else if (msg is AppendEntriesRpc) {
//            val rpc: AppendEntriesRpc = msg as AppendEntriesRpc
//            val protoRpc: AppendEntriesRpc = Protos.AppendEntriesRpc.newBuilder()
//                .setMessageId(rpc.getMessageId())
//                .setTerm(rpc.getTerm())
//                .setLeaderId(rpc.getLeaderId().getValue())
//                .setLeaderCommit(rpc.getLeaderCommit())
//                .setPrevLogIndex(rpc.getPrevLogIndex())
//                .setPrevLogTerm(rpc.getPrevLogTerm())
//                .addAllEntries(
//                    rpc.getEntries().stream().map { e ->
//                        Protos.AppendEntriesRpc.Entry.newBuilder()
//                            .setKind(e.getKind())
//                            .setIndex(e.getIndex())
//                            .setTerm(e.getTerm())
//                            .setCommand(ByteString.copyFrom(e.getCommandBytes()))
//                            .build()
//                    }.collect(Collectors.toList())
//                ).build()
//            this.writeMessage(out, MessageConstants.MSG_TYPE_APPEND_ENTRIES_RPC, protoRpc)
//        } else if (msg is AppendEntriesResult) {
//            val result: AppendEntriesResult = msg as AppendEntriesResult
//            val protoResult: AppendEntriesResult = Protos.AppendEntriesResult.newBuilder()
//                .setRpcMessageId(result.getRpcMessageId())
//                .setTerm(result.getTerm())
//                .setSuccess(result.isSuccess())
//                .build()
//            this.writeMessage(out, MessageConstants.MSG_TYPE_APPEND_ENTRIES_RESULT, protoResult)
//        } else if (msg is InstallSnapshotRpc) {
//            val rpc: InstallSnapshotRpc = msg as InstallSnapshotRpc
//            val protoRpc: Protos.InstallSnapshotRpc = Protos.InstallSnapshotRpc.newBuilder()
//                .setTerm(rpc.getTerm())
//                .setLeaderId(rpc.getLeaderId().getValue())
//                .setLastIndex(rpc.getLastIndex())
//                .setLastTerm(rpc.getLastTerm())
//                .addAllLastConfig(
//                    rpc.getLastConfig().stream().map { e ->
//                        Protos.NodeEndpoint.newBuilder()
//                            .setId(e.getId().getValue())
//                            .setHost(e.getHost())
//                            .setPort(e.getPort())
//                            .build()
//                    }.collect(Collectors.toList())
//                )
//                .setOffset(rpc.getOffset())
//                .setData(ByteString.copyFrom(rpc.getData()))
//                .setDone(rpc.isDone()).build()
//            this.writeMessage(out, MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_PRC, protoRpc)
//        } else if (msg is Protos.InstallSnapshotResult) {
//            val result: Protos.InstallSnapshotResult = msg as InstallSnapshotResult
//            val protoResult: kites.raft.Protos.InstallSnapshotResult? = Protos.InstallSnapshotResult.newBuilder()
//                .setTerm(result.getTerm()).build()
//            this.writeMessage(out, MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_RESULT, protoResult)
//        }
    }

//    @Throws(IOException::class)
//    private fun writeMessage(out: ByteBuf, messageType: Int, message: MessageLite) {
//        val byteOutput = ByteArrayOutputStream()
//        message.writeTo(byteOutput)
//        out.writeInt(messageType)
//        this.writeBytes(out, byteOutput.toByteArray())
//    }

    private fun writeMessage(out: ByteBuf, messageType: Int, bytes: ByteArray) {
        // 4 + 4 + VAR
        out.writeInt(messageType)
        this.writeBytes(out, bytes)
    }

    private fun writeBytes(out: ByteBuf, bytes: ByteArray) {
        out.writeInt(bytes.size)
        out.writeBytes(bytes)
    }
}

