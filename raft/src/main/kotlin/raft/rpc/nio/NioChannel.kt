package raft.rpc.nio


import io.netty.channel.ChannelException
import raft.rpc.Channel
import raft.rpc.message.*


class NioChannel(
    val nettyChannel: io.netty.channel.Channel
) : Channel {

    override fun writeRequestVoteRpc(rpc: RequestVoteRpc) {
        nettyChannel.writeAndFlush(rpc)
    }

    override fun writeRequestVoteResult(result: RequestVoteResult) {
        nettyChannel.writeAndFlush(result)
    }

    override fun writeAppendEntriesRpc(rpc: AppendEntriesRpc) {
        nettyChannel.writeAndFlush(rpc)
    }

    override fun writeAppendEntriesResult(result: AppendEntriesResult) {
        nettyChannel.writeAndFlush(result)
    }


    override fun writeInstallSnapshotRpc(rpc: InstallSnapshotRpc) {
        nettyChannel.writeAndFlush(rpc)
    }

    override fun writeInstallSnapshotResult(result: InstallSnapshotResult) {
        nettyChannel.writeAndFlush(result)
    }

    override fun close() {
        try {
            nettyChannel.close().sync()
        } catch (e: InterruptedException) {
            throw ChannelException("failed to close", e)
        }
    }
}

