package raft.rpc

import raft.node.NodeEndpoint
import raft.node.NodeId
import raft.rpc.message.*
import java.util.*
import javax.annotation.Nonnull


class MockConnector : Connector {

    private val messages: LinkedList<Message> = LinkedList<Message>()


    public class Message {

        internal lateinit var rpc: Any
        internal lateinit var destinationNodeId: NodeId
        internal lateinit var result: Any

        fun getRpc(): Any? {
            return rpc
        }

        fun getDestinationNodeId(): NodeId? {
            return destinationNodeId
        }

        fun getResult(): Any? {
            return result
        }

        override fun toString(): String {
            return "Message{" +
                    "destinationNodeId=" + destinationNodeId +
                    ", rpc=" + rpc +
                    ", result=" + result +
                    '}'
        }


    }

    override fun initialize() {
        TODO("Not yet implemented")
    }

    override fun sendRequestVote(@Nonnull rpc: RequestVoteRpc, @Nonnull destinationEndpoints: Collection<NodeEndpoint>) {
        val m: Message = Message()
        m.rpc = rpc
        messages.add(m)
    }

    override fun replyRequestVote(result: RequestVoteResult, rpcMessage: RequestVoteRpcMessage) {
        val m = Message()
        m.result = result
        m.destinationNodeId = rpcMessage.sourceNodeId
        messages.add(m)
    }

    override fun sendAppendEntries(rpc: AppendEntriesRpc, destinationEndpoint: NodeEndpoint) {
        val m: Message = Message()
        m.rpc = rpc
        m.destinationNodeId = destinationEndpoint.id
        messages.add(m)
    }

    override fun replyAppendEntries(result: AppendEntriesResult, rpcMessage: AppendEntriesRpcMessage) {

        val m: Message = Message()
        m.result = result
        m.destinationNodeId = rpcMessage.sourceNodeId
        messages.add(m)
    }

    override fun close() {

    }

    override fun resetChannels() {
        TODO("Not yet implemented")
    }

    fun getLastMessage(): Message? {
        return if (messages.isEmpty()) null else messages.last
    }

    private fun getLastMessageOrDefault(): Message? {
        return if (messages.isEmpty()) Message() else messages.last
    }

    fun getRpc(): Any {
        return getLastMessageOrDefault()!!.rpc
    }

    fun getResult(): Any? {
        return getLastMessageOrDefault()!!.result
    }

    fun getDestinationNodeId(): NodeId? {
        return getLastMessageOrDefault()!!.destinationNodeId
    }

    fun getMessageCount(): Int {
        return messages.size
    }

    fun getMessages(): List<Message?>? {
        return ArrayList(messages)
    }

    fun clearMessage() {
        messages.clear()
    }

}


