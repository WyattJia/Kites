package raft.rpc.nio


import io.netty.channel.ChannelException
import raft.rpc.Channel
import raft.rpc.message.AppendEntriesResult
import raft.rpc.message.AppendEntriesRpc
import raft.rpc.message.RequestVoteResult
import raft.rpc.message.RequestVoteRpc


class NioChannel(
    val nettyChannel: io.netty.channel.Channel
) : Channel {

    override fun writeRequestVoteRpc(rpc: RequestVoteRpc?) {
        nettyChannel.writeAndFlush(rpc)
    }

    override fun writeRequestVoteResult(result: RequestVoteResult?) {
        nettyChannel.writeAndFlush(result)
    }

    override fun writeAppendEntriesRpc(rpc: AppendEntriesRpc?) {
        nettyChannel.writeAndFlush(rpc)
    }

    override fun writeAppendEntriesResult(result: AppendEntriesResult?) {
        nettyChannel.writeAndFlush(result)
    }


//    fun writeInstallSnapshotRpc(@Nonnull rpc: InstallSnapshotRpc?) {
//        delegate.writeAndFlush(rpc)
//    }
//
//    fun writeInstallSnapshotResult(@Nonnull result: InstallSnapshotResult?) {
//        delegate.writeAndFlush(result)
//    }

    override fun close() {
        try {
            nettyChannel.close().sync()
        } catch (e: InterruptedException) {
            throw ChannelException("failed to close", e)
        }
    }
}

