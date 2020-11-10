package kvstore.client

import raft.node.Address
import raft.node.NodeId
import java.io.Console

class ConsoleLauncher {
    private class ServerConfig internal constructor(val nodeId: String, val host: String, val port: Int)

    private fun execute(args: Array<String>) {
        val options = Options()
        options.addOption(
            Option.builder("gc")
                .hasArgs()
                .argName("server-config")
                .required()
                .desc(
                    "group config, required. format: <server-config> <server-config>. " +
                            "format of server config: <node-id>,<host>,<port-service>. e.g A,localhost,8001 B,localhost,8011"
                )
                .build()
        )
        if (args.size == 0) {
            val formatter = HelpFormatter()
            formatter.printHelp("xraft-kvstore-client [OPTION]...", options)
            return
        }
        val parser: CommandLineParser = DefaultParser()
        val serverMap: Map<NodeId, Address>
        serverMap = try {
            val commandLine: CommandLine = parser.parse(options, args)
            parseGroupConfig(commandLine.getOptionValues("gc"))
        } catch (e: ParseException) {
            System.err.println(e.getMessage())
            return
        } catch (e: IllegalArgumentException) {
            System.err.println(e.message)
            return
        }
        val console = Console(serverMap)
        console.start()
    }

    private fun parseGroupConfig(rawGroupConfig: Array<String>): Map<NodeId, Address> {
        val serverMap: MutableMap<NodeId, Address> = HashMap<NodeId, Address>()
        for (rawServerConfig in rawGroupConfig) {
            val serverConfig = parseServerConfig(rawServerConfig)
            serverMap[NodeId(serverConfig.nodeId)] = Address(serverConfig.host, serverConfig.port)
        }
        return serverMap
    }

    private fun parseServerConfig(rawServerConfig: String): ServerConfig {
        val pieces = rawServerConfig.split(",").toTypedArray()
        require(pieces.size == 3) { "illegal server config [$rawServerConfig]" }
        val nodeId = pieces[0]
        val host = pieces[1]
        val port: Int
        port = try {
            pieces[2].toInt()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("illegal port [" + pieces[2] + "]")
        }
        return ServerConfig(nodeId, host, port)
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val launcher = ConsoleLauncher()
            launcher.execute(args)
        }
    }
}

