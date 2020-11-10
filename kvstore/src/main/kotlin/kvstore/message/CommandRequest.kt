package kvstore.message

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener


class CommandRequest<T>(val command: T, channel: Channel) {
    private val channel: Channel = channel
    fun reply(response: Any?) {
        channel.writeAndFlush(response)
    }

    fun addCloseListener(runnable: Runnable) {
        channel.closeFuture()
            .addListener(ChannelFutureListener { future: ChannelFuture? -> runnable.run() } as ChannelFutureListener?)
    }

}

