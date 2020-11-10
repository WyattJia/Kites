package kvstore.client

import raft.node.Address
import raft.node.NodeId
import raft.service.ServerRouter


class CommandContext(serverMap: MutableMap<NodeId, Address>) {
    private val serverMap: MutableMap<NodeId, Address> = serverMap
    private var client: Client
    var isRunning = false

    private fun buildServerRouter(serverMap: Map<NodeId, Address>): ServerRouter {
        val router = ServerRouter()
        for (nodeId in serverMap.keys) {
            val address: Address = serverMap[nodeId]!!
            router.add(nodeId, SocketChannel(address.host, address.port))
        }
        return router
    }

    fun getClient(): Client {
        return client
    }

    var clientLeader: NodeId
        get() = client.getServerRouter().getLeaderId()!!
        set(nodeId) {
            client.getServerRouter().setLeaderId(nodeId)
        }

    fun clientAddServer(nodeId: String?, host: String?, portService: Int) {
        serverMap[nodeId?.let { NodeId(it) }!!] = host?.let { Address(it, portService) }!!
        client = Client(buildServerRouter(serverMap))
    }

    fun clientRemoveServer(nodeId: String?): Boolean {
        val address: Address? = serverMap.remove(nodeId?.let { NodeId(it) })
        if (address != null) {
            client = Client(buildServerRouter(serverMap))
            return true
        }
        return false
    }

    fun printSeverList() {
        for (nodeId in serverMap.keys) {
            val address: Address? = serverMap[nodeId]
            println(nodeId.toString() + "," + address!!.host + "," + address.port)
        }
    }

    init {
        client = Client(buildServerRouter(serverMap))
    }
}

