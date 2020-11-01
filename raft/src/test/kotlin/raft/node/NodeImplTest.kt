package raft.node

import org.junit.Assert
import org.junit.Test
import raft.node.NodeEndpoint.NodeEndpoint
import raft.node.role.LeaderNodeRole
import raft.node.role.RoleState
import raft.node.store.MemoryNodeStore
import raft.rpc.MockConnector
import raft.rpc.message.*
import raft.schedule.NullScheduler
import raft.support.DirectTaskExecutor
import java.util.stream.Collectors


class NodeImplTest {

    private fun newNodeBuilder(selfId: NodeId, vararg endpoints: NodeEndpoint): NodeBuilder? {
        return NodeBuilder(listOf(*endpoints), selfId)
            .setScheduler(NullScheduler())
            .setConnector(MockConnector())
            .setTaskExecutor(DirectTaskExecutor(true))
    }


    @Test
    fun testStart() {
        val node = newNodeBuilder(NodeId("A"), NodeEndpoint("A", "localhost", 2333))
            ?.build() as NodeImpl
        node.start()

        val state: RoleState = node.role.state!!
        Assert.assertEquals(1, state.term)
        Assert.assertNull(state.votedFor)
    }

    @Test
    fun testElectionTimeoutWhenFollower() {
        val node = newNodeBuilder(
            NodeId("A"),
            NodeEndpoint("A", "localhost", 2333),
            NodeEndpoint("B", "localhost", 2334),
            NodeEndpoint("C", "localhost", 2335)
        )!!.build() as NodeImpl
        node.start()
        node.electionTimeout()
        val state: RoleState = node.role.state!!
        Assert.assertEquals(RoleName.CANDIDATE, state.roleName)
        Assert.assertEquals(1, state.term)
        Assert.assertEquals(1, state.votesCount)
        val mockConnector = node.context.connector as MockConnector
        val rpc: RequestVoteRpc = mockConnector.getRpc() as RequestVoteRpc
        Assert.assertEquals(1, rpc.term)
        Assert.assertEquals(NodeId("A"), rpc.candidateId)
        Assert.assertEquals(0, rpc.lastLogIndex)
        Assert.assertEquals(0, rpc.lastLogTerm)
    }

    @Test
    fun testOnReceiveRequestVoteRpcFollower() {
        val node = newNodeBuilder(
            NodeId("A"),
            NodeEndpoint("A", "localhost", 2333),
            NodeEndpoint("B", "localhost", 2334),
            NodeEndpoint("C", "localhost", 2335)
        )?.setStore(MemoryNodeStore(1, null))
            ?.build() as NodeImpl
        node.start()
        val rpc = RequestVoteRpc()
        rpc.term = 1
        rpc.candidateId = NodeId("C")
        rpc.lastLogIndex = 0
        rpc.lastLogTerm = 0
        node.onReceiveRequestVoteRpc(RequestVoteRpcMessage(rpc, NodeId("C"), null))
        val mockConnector = node.context.connector as MockConnector
        val result: RequestVoteResult = mockConnector.getResult() as RequestVoteResult
        Assert.assertEquals(1, result.getTerm())
        Assert.assertTrue(result.isVoteGranted())
        Assert.assertEquals(NodeId("C"), node.role.state?.votedFor)
    }

    @Test
    fun testOnReceiveRequestVoteResult() {
        val node = newNodeBuilder(
            NodeId("A"),
            NodeEndpoint("A", "localhost", 2333),
            NodeEndpoint("B", "localhost", 2334),
            NodeEndpoint("C", "localhost", 2335)
        )!!.build() as NodeImpl
        node.start()
        node.electionTimeout()
        node.onReceiveRequestVoteResult(RequestVoteResult(1, true))

        val role = node.role as LeaderNodeRole
        Assert.assertEquals(1, role.term)
    }

    @Test
    fun testReplicateLog() {
        val node = newNodeBuilder(
            NodeId("A"),
            NodeEndpoint("A", "localhost", 2333),
            NodeEndpoint("B", "localhost", 2334),
            NodeEndpoint("C", "localhost", 2335)
        )!!.build() as NodeImpl
        node.start()

        node.electionTimeout() // request vote rpc
        node.onReceiveRequestVoteResult(RequestVoteResult(1, true))
        node.replicateLog() // append entries * 2
        val mockConnector = node.context.connector as MockConnector
        Assert.assertEquals(3, mockConnector.getMessageCount())

        // check destination node id

        // check destination node id
        val messages = mockConnector.getMessages()
        val destinationNodeIds = messages!!.subList(1, 3).stream()
//            .map<Any>(MockConnector.Message::getDestinationNodeId)
            .collect(Collectors.toSet<Any>())
        Assert.assertEquals(2, destinationNodeIds.size.toLong())
        Assert.assertTrue(destinationNodeIds.contains(NodeId("B")))
        Assert.assertTrue(destinationNodeIds.contains(NodeId("C")))
        val rpc: AppendEntriesRpc = messages[2]!!.getRpc() as AppendEntriesRpc
        Assert.assertEquals(1, rpc.term)
    }

    @Test
    fun testOnReceiveAppendEntriesRpcFollower() {
        val node = newNodeBuilder(
            NodeId("A"),
            NodeEndpoint("A", "localhost", 2333),
            NodeEndpoint("B", "localhost", 2334),
            NodeEndpoint("C", "localhost", 2335)
        )
            ?.build() as NodeImpl
        node.start()
        val rpc = AppendEntriesRpc()
        rpc.term = (1)
        rpc.leaderId = NodeId("B")
        node.onReceiveAppendEntriesRpc(AppendEntriesRpcMessage(rpc, NodeId("B"), null))
        val connector = node.context.connector as MockConnector
        val result: AppendEntriesResult? = connector.getResult() as AppendEntriesResult?
        if (result != null) {
            Assert.assertEquals(1, result.getTerm())
        }
        if (result != null) {
            Assert.assertTrue(result.isSuccess())
        }
        val state: RoleState = node.role.state!!
        Assert.assertEquals(RoleName.FOLLOWER, state.roleName)
        Assert.assertEquals(1, state.term)
        Assert.assertEquals(NodeId("B"), state.leaderId)
    }

    @Test
    fun testOnReceiveAppendEntriesNormal() {
        val node = newNodeBuilder(
            NodeId("A"),
            NodeEndpoint("A", "localhost", 2333),
            NodeEndpoint("B", "localhost", 2334),
            NodeEndpoint("C", "localhost", 2335)
        )
            ?.build() as NodeImpl
        node.start()
        node.electionTimeout()
        node.onReceiveRequestVoteResult(RequestVoteResult(1, true))
        node.replicateLog()
        node.onReceiveAppendEntriesResult(
            AppendEntriesResultMessage(
                AppendEntriesResult(1, true),
                NodeId("B"),
                AppendEntriesRpc()
            )
        )
    }
}
