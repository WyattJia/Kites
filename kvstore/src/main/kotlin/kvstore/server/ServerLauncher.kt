package kvstore.server

import jdk.internal.joptsimple.HelpFormatter
import org.apache.commons.cli.*
import org.slf4j.LoggerFactory
import raft.node.Node
import raft.node.NodeBuilder
import raft.node.NodeEndpoint
import raft.node.NodeId
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

// TODO load config from file
class ServerLauncher {
    // TODO why volatile?
    @Volatile
    private var server: Server? = null
    @Throws(Exception::class)
    private fun execute(args: Array<String>) {
        val options = Options()
        options.addOption(
            Option.builder("m")
                .hasArg()
                .argName("mode")
                .desc("start mode, available: standalone, standby, group-member. default is standalone")
                .build()
        )
        options.addOption(
            Option.builder("i")
                .longOpt("id")
                .hasArg()
                .argName("node-id")
                .required()
                .desc(
                    "node id, required. must be unique in group. " +
                            "if starts with mode group-member, please ensure id in group config"
                )
                .build()
        )
        options.addOption(
            Option.builder("h")
                .hasArg()
                .argName("host")
                .desc("host, required when starts with standalone or standby mode")
                .build()
        )
        options.addOption(
            Option.builder("p1")
                .longOpt("port-raft-node")
                .hasArg()
                .argName("port")
                .type(Number::class.java)
                .desc("port of raft node, required when starts with standalone or standby mode")
                .build()
        )
        options.addOption(
            Option.builder("p2")
                .longOpt("port-service")
                .hasArg()
                .argName("port")
                .type(Number::class.java)
                .required()
                .desc("port of service, required")
                .build()
        )
        options.addOption(
            Option.builder("d")
                .hasArg()
                .argName("data-dir")
                .desc("data directory, optional. must be present")
                .build()
        )
        options.addOption(
            Option.builder("gc")
                .hasArgs()
                .argName("node-endpoint")
                .desc(
                    "group config, required when starts with group-member mode. format: <node-endpoint> <node-endpoint>..., " +
                            "format of node-endpoint: <node-id>,<host>,<port-raft-node>, eg: A,localhost,8000 B,localhost,8010"
                )
                .build()
        )
        if (args.size == 0) {
            val formatter = HelpFormatter()
            formatter.printHelp("kites-kvstore [OPTION]...", options)
            return
        }
        val parser: CommandLineParser = DefaultParser()
        try {
            val cmdLine: CommandLine = parser.parse(options, args)
            val mode: String = cmdLine.getOptionValue('m', MODE_STANDALONE)
            when (mode) {
                MODE_STANDBY -> startAsStandaloneOrStandby(cmdLine, true)
                MODE_STANDALONE -> startAsStandaloneOrStandby(cmdLine, false)
                MODE_GROUP_MEMBER -> startAsGroupMember(cmdLine)
                else -> throw IllegalArgumentException("illegal mode [$mode]")
            }
        } catch (e: ParseException) {
            System.err.println(e.getMessage())
        } catch (e: IllegalArgumentException) {
            System.err.println(e.getMessage())
        }
    }

    @Throws(Exception::class)
    private fun startAsStandaloneOrStandby(cmdLine: CommandLine, standby: Boolean) {
        require(!(!cmdLine.hasOption("p1") || !cmdLine.hasOption("p2"))) { "port-raft-node or port-service required" }
        val id: String = cmdLine.getOptionValue('i')
        val host: String = cmdLine.getOptionValue('h', "localhost")
        val portRaftServer = (cmdLine.getParsedOptionValue("p1") as Long).toInt()
        val portService = (cmdLine.getParsedOptionValue("p2") as Long).toInt()
        val nodeEndpoint = NodeEndpoint(id, host, portRaftServer)
        val node: Node = NodeBuilder(nodeEndpoint)
            .setStandby(standby)
            .setDataDir(cmdLine.getOptionValue('d'))
            .build()
        val server = Server(node, portService)
        logger.info(
            "start with mode {}, id {}, host {}, port raft node {}, port service {}",
            if (standby) "standby" else "standalone", id, host, portRaftServer, portService
        )
        startServer(server)
    }

    @Throws(Exception::class)
    private fun startAsGroupMember(cmdLine: CommandLine) {
        require(cmdLine.hasOption("gc")) { "group-config required" }
        val rawGroupConfig: Array<String> = cmdLine.getOptionValues("gc")
        val rawNodeId: String = cmdLine.getOptionValue('i')
        val portService = (cmdLine.getParsedOptionValue("p2") as Long).toInt()
        val nodeEndpoints: Set<NodeEndpoint> = Stream.of(*rawGroupConfig)
            .map(Function<String, R> { rawNodeEndpoint: String ->
                parseNodeEndpoint(
                    rawNodeEndpoint
                )
            })
            .collect<Set<NodeEndpoint>, Any>(Collectors.toSet<Any>())
        val node: Node = NodeBuilder(nodeEndpoints, NodeId(rawNodeId))
            .setDataDir(cmdLine.getOptionValue('d'))
            .build()
        val server = Server(node, portService)
        logger.info(
            "start as group member, group config {}, id {}, port service {}",
            nodeEndpoints,
            rawNodeId,
            portService
        )
        startServer(server)
    }

    private fun parseNodeEndpoint(rawNodeEndpoint: String): NodeEndpoint {
        val pieces = rawNodeEndpoint.split(",").toTypedArray()
        require(pieces.size == 3) { "illegal node endpoint [$rawNodeEndpoint]" }
        val nodeId = pieces[0]
        val host = pieces[1]
        val port: Int
        port = try {
            pieces[2].toInt()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("illegal port in node endpoint [$rawNodeEndpoint]")
        }
        return NodeEndpoint(nodeId, host, port)
    }

    @Throws(Exception::class)
    private fun startServer(server: Server) {
        this.server = server
        this.server!!.start()
        Runtime.getRuntime().addShutdownHook(Thread({ stopServer() }, "shutdown"))
    }

    private fun stopServer() {
        try {
            server!!.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServerLauncher::class.java)
        private const val MODE_STANDALONE = "standalone"
        private const val MODE_STANDBY = "standby"
        private const val MODE_GROUP_MEMBER = "group-member"
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val launcher = ServerLauncher()
            launcher.execute(args)
        }
    }
}

