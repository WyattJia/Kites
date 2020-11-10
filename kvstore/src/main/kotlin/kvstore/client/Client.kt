package kvstore.client

import kvstore.message.GetCommand
import kvstore.message.SetCommand
import raft.service.AddNodeCommand
import raft.service.RemoveNodeCommand
import raft.service.ServerRouter


class Client(serverRouter: ServerRouter) {
    private val serverRouter: ServerRouter
    fun addNote(nodeId: String?, host: String?, port: Int) {
        serverRouter.send(AddNodeCommand(nodeId, host, port))
    }

    fun removeNode(nodeId: String?) {
        serverRouter.send(RemoveNodeCommand(nodeId))
    }

    operator fun set(key: String?, value: ByteArray?) {
        serverRouter.send(SetCommand(key!!, value!!))
    }

    operator fun get(key: String?): ByteArray {
        return key?.let { GetCommand(it) }?.let { serverRouter.send(it) } as ByteArray
    }

    fun getServerRouter(): ServerRouter {
        return serverRouter
    }

    companion object {
        const val VERSION = "0.1.0"
    }

    init {
        this.serverRouter = serverRouter
    }
}

