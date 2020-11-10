package kvstore.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.LoggerFactory
import raft.node.Node

class Server(node: Node, port: Int) {
    private val node: Node
    private val port: Int
    private val service: Service
    private val bossGroup: NioEventLoopGroup = NioEventLoopGroup(1)
    private val workerGroup: NioEventLoopGroup = NioEventLoopGroup(4)
    @Throws(Exception::class)
    fun start() {
        node.start()
        val serverBootstrap: ServerBootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel?>() {
                @Throws(Exception::class)
                protected override fun initChannel(ch: SocketChannel?) {
                    val pipeline: ChannelPipeline = ch.pipeline()
                    pipeline.addLast(Encoder())
                    pipeline.addLast(Decoder())
                    pipeline.addLast(ServiceHandler(service))
                }
            })
        logger.info("server started at port {}", port)
        serverBootstrap.bind(port)
    }

    @Throws(Exception::class)
    fun stop() {
        logger.info("stopping server")
        node.stop()
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
    }

    init {
        this.node = node
        service = Service(node)
        this.port = port
    }
}
