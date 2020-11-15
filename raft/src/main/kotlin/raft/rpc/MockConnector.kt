package raft.rpc

import raft.node.NodeEndpoint
import raft.node.NodeId
import raft.rpc.message.*
import java.util.*


class MockConnector : ConnectorAdapter() {
    private val messages = LinkedList<Message>()
    override fun sendRequestVote(rpc: RequestVoteRpc, destinationEndpoints: Collection<NodeEndpoint>) {
        val m = Message()
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
        val m = Message()
        m.rpc = rpc
        m.destinationNodeId = destinationEndpoint.id
        messages.add(m)
    }

    override fun replyAppendEntries(result: AppendEntriesResult, rpcMessage: AppendEntriesRpcMessage) {
        val m = Message()
        m.result = result
        m.destinationNodeId = rpcMessage.sourceNodeId
        messages.add(m)
    }

    override fun sendInstallSnapshot(rpc: InstallSnapshotRpc, destinationEndpoint: NodeEndpoint) {
        val m = Message()
        m.rpc = rpc
        m.destinationNodeId = destinationEndpoint.id
        messages.add(m)
    }

    override fun replyInstallSnapshot(result: InstallSnapshotResult, rpcMessage: InstallSnapshotRpcMessage) {
        val m = Message()
        m.result = result
        m.destinationNodeId = rpcMessage.sourceNodeId
        messages.add(m)
    }

    val lastMessage: Message?
        get() = if (messages.isEmpty()) null else messages.last
    private val lastMessageOrDefault: Message
        get() = if (messages.isEmpty()) Message() else messages.last
    val rpc: Any?
        get() = lastMessageOrDefault.rpc
    val result: Any?
        get() = lastMessageOrDefault.result
    val destinationNodeId: NodeId?
        get() = lastMessageOrDefault.destinationNodeId
    val messageCount: Int
        get() = messages.size

    fun getMessages(): List<Message> {
        return ArrayList(messages)
    }

    fun clearMessage() {
        messages.clear()
    }

    class Message {
        var rpc: Any? = null
        var destinationNodeId: NodeId? = null
        var result: Any? = null

        override fun toString(): String {
            return "Message{" +
                    "destinationNodeId=" + destinationNodeId +
                    ", rpc=" + rpc +
                    ", result=" + result +
                    '}'
        }
    }
}


