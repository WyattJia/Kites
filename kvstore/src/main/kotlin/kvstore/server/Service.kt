package kvstore.server

import kvstore.message.*
import raft.log.statemachine.AbstractSingleThreadStateMachine
import raft.node.Node
import raft.node.RoleName
import raft.node.role.RoleNameAndLeaderId
import raft.node.task.GroupConfigChangeTaskResult
import raft.service.RemoveNodeCommand
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeoutException


class Service(node: Node) {
    private val node: Node
    private val pendingCommands: ConcurrentMap<String, CommandRequest<*>> =
        ConcurrentHashMap<String, CommandRequest<*>>()
    private var map: MutableMap<String, ByteArray> = HashMap()
    fun addNode(commandRequest: CommandRequest<AddNodeCommand>) {
        val redirect: Redirect? = checkLeadership()
        if (redirect != null) {
            commandRequest.reply(redirect)
            return
        }
        val command: AddNodeCommand = commandRequest.command
        val taskReference: GroupConfigChangeTaskReference = node.addNode(command.toNodeEndpoint())
        awaitResult<Any>(taskReference, commandRequest)
    }

    private fun <T> awaitResult(taskReference: GroupConfigChangeTaskReference, commandRequest: CommandRequest<RemoveNodeCommand>) {
        try {
            when (taskReference.getResult(3000L)) {
                GroupConfigChangeTaskResult.OK -> commandRequest.reply(Success)
                GroupConfigChangeTaskResult.TIMEOUT -> commandRequest.reply(Failure(101, "timeout"))
                else -> commandRequest.reply(Failure(100, "error"))
            }
        } catch (e: TimeoutException) {
            commandRequest.reply(Failure(101, "timeout"))
        } catch (ignored: InterruptedException) {
            commandRequest.reply(Failure(100, "error"))
        }
    }

    fun removeNode(commandRequest: CommandRequest<RemoveNodeCommand>) {
        val redirect: Redirect? = checkLeadership()
        if (redirect != null) {
            commandRequest.reply(redirect)
            return
        }
        val command: RemoveNodeCommand = commandRequest.command
        val taskReference: GroupConfigChangeTaskReference = node.removeNode(command.getNodeId())
        awaitResult<Any>(taskReference, commandRequest)
    }

    fun set(commandRequest: CommandRequest<SetCommand?>) {
        val redirect: Redirect? = checkLeadership()
        if (redirect != null) {
            commandRequest.reply(redirect)
            return
        }
        val command: SetCommand = commandRequest.command!!
        logger.debug("set {}", command.key)
        pendingCommands[command.requestId] = commandRequest
        commandRequest.addCloseListener { pendingCommands.remove(command.requestId) }
        node.appendLog(command.toBytes())
    }

    operator fun get(commandRequest: CommandRequest<GetCommand?>) {
        val key: String = commandRequest.command!!.key
        logger.debug("get {}", key)
        val value = map[key]
        // TODO view from node state machine
        commandRequest.reply(GetCommandResponse(value))
    }

    private fun checkLeadership(): Redirect? {
        val state: RoleNameAndLeaderId = node.roleNameAndLeaderId!!
        return if (state.roleName != RoleName.LEADER) {
            Redirect(state.leaderId)
        } else null
    }

    private inner class StateMachineImpl : AbstractSingleThreadStateMachine() {
        protected override fun applyCommand(commandBytes: ByteArray?) {
            val command: SetCommand = SetCommand.fromBytes(commandBytes)
            map[command.key] = command.value
            val commandRequest: CommandRequest<*>? = pendingCommands.remove(command.requestId)
            if (commandRequest != null) {
                commandRequest.reply(Success)
            }
        }

        @Throws(IOException::class)
        protected override fun doApplySnapshot(input: InputStream?) {
            map = fromSnapshot(input)
        }

        override fun shouldGenerateSnapshot(firstLogIndex: Int, lastApplied: Int): Boolean {
            return lastApplied - firstLogIndex > 1
        }

        @Throws(IOException::class)
        override fun generateSnapshot(output: OutputStream?) {
            toSnapshot(map, output)
        }
    }

    companion object {
        private val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(Service::class.java)
        @Throws(IOException::class)
        fun toSnapshot(map: Map<String, ByteArray>, output: OutputStream?) {
            val entryList: Protos.EntryList.Builder = Protos.EntryList.newBuilder()
            for ((key, value) in map) {
                entryList.addEntries(
                    Protos.EntryList.Entry.newBuilder()
                        .setKey(key)
                        .setValue(ByteString.copyFrom(value)).build()
                )
            }
            entryList.build().writeTo(output)
            entryList.build().getSerializedSize()
        }

        @Throws(IOException::class)
        fun fromSnapshot(input: InputStream?): MutableMap<String, ByteArray> {
            val map: MutableMap<String, ByteArray> = HashMap()
            val entryList: Protos.EntryList = Protos.EntryList.parseFrom(input)
            for (entry in entryList.getEntriesList()) {
                map[entry.getKey()] = entry.getValue().toByteArray()
            }
            return map
        }
    }

    init {
        this.node = node
        this.node.registerStateMachine(StateMachineImpl())
    }
}

