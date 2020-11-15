package raft.rpc.nio

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import raft.log.entry.EntryFactory


class Decoder : ByteToMessageDecoder() {
    private val entryFactory = EntryFactory()

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf?, out: MutableList<Any>?) {
        TODO("Not yet implemented")
    }

//    @Throws(Exception::class)
//    override fun decode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, out: MutableList<Any>) {
//        val availableBytes = byteBuf.readableBytes()
//        if (availableBytes < 8) return
//        byteBuf.markReaderIndex()
//        val messageType = byteBuf.readInt()
//        val payloadLength = byteBuf.readInt()
//        if (byteBuf.readableBytes() < payloadLength) {
//            byteBuf.resetReaderIndex()
//            return
//        }
//        val payload = ByteArray(payloadLength)
//        byteBuf.readBytes(payload)
//        when (messageType) {
//            MessageConstants.MSG_TYPE_NODE_ID -> out.add(NodeId(String(payload)))
//            MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC -> {
//                val protoRVRpc: Protos.RequestVoteRpc? = Protos.RequestVoteRpc.parseFrom(payload)
//                val rpc = RequestVoteRpc()
//                rpc.term = protoRVRpc!!.term
//                rpc.candidateId = NodeId(protoRVRpc!!.candidateId)
//                rpc.lastLogIndex = protoRVRpc!!.lastLogIndex
//                rpc.lastLogTerm = protoRVRpc!!.lastLogTerm
//                out.add(rpc)
//            }
//            MessageConstants.MSG_TYPE_REQUEST_VOTE_RESULT -> {
//                val protoRVResult: Protos.RequestVoteResult = Protos.RequestVoteResult.parseFrom(payload)
//                out.add(RequestVoteResult(protoRVResult.getTerm(), protoRVResult.getVoteGranted()))
//            }
//            MessageConstants.MSG_TYPE_APPEND_ENTRIES_RPC -> {
//                val protoAERpc: Protos.AppendEntriesRpc = Protos.AppendEntriesRpc.parseFrom(payload)
//                val aeRpc = AppendEntriesRpc()
//                aeRpc.setMessageId(protoAERpc.getMessageId())
//                aeRpc.setTerm(protoAERpc.getTerm())
//                aeRpc.setLeaderId(NodeId(protoAERpc.getLeaderId()))
//                aeRpc.setLeaderCommit(protoAERpc.getLeaderCommit())
//                aeRpc.setPrevLogIndex(protoAERpc.getPrevLogIndex())
//                aeRpc.setPrevLogTerm(protoAERpc.getPrevLogTerm())
//                aeRpc.setEntries(protoAERpc.getEntriesList().stream().map { e ->
//                    entryFactory.create(
//                        e.getKind(),
//                        e.getIndex(),
//                        e.getTerm(),
//                        e.getCommand().toByteArray()
//                    )
//                }.collect(Collectors.toList()))
//                out.add(aeRpc)
//            }
//            MessageConstants.MSG_TYPE_APPEND_ENTRIES_RESULT -> {
//                val protoAEResult: Protos.AppendEntriesResult = Protos.AppendEntriesResult.parseFrom(payload)
//                out.add(
//                    AppendEntriesResult(
//                        protoAEResult.getRpcMessageId(),
//                        protoAEResult.getTerm(),
//                        protoAEResult.getSuccess()
//                    )
//                )
//            }
//            MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_PRC -> {
//                val protoISRpc: Protos.InstallSnapshotRpc = Protos.InstallSnapshotRpc.parseFrom(payload)
//                val isRpc = InstallSnapshotRpc()
//                isRpc.setTerm(protoISRpc.getTerm())
//                isRpc.setLeaderId(NodeId(protoISRpc.getLeaderId()))
//                isRpc.setLastIndex(protoISRpc.getLastIndex())
//                isRpc.setLastTerm(protoISRpc.getTerm())
//                isRpc.setLastConfig(protoISRpc.getLastConfigList().stream().map { e ->
//                    NodeEndpoint(
//                        e.getId(),
//                        e.getHost(),
//                        e.getPort()
//                    )
//                }.collect(Collectors.toSet()))
//                isRpc.setOffset(protoISRpc.getOffset())
//                isRpc.setData(protoISRpc.getData().toByteArray())
//                isRpc.setDone(protoISRpc.getDone())
//                out.add(isRpc)
//            }
//            MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_RESULT -> {
//                val protoISResult: Protos.InstallSnapshotResult = Protos.InstallSnapshotResult.parseFrom(payload)
//                out.add(InstallSnapshotResult(protoISResult.getTerm()))
//            }
//        }
//    }
}

