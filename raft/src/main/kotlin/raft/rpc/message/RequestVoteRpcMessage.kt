package raft.rpc.message

import raft.node.NodeId
import raft.rpc.Channel


class RequestVoteRpcMessage(rpc: RequestVoteRpc?, sourceNodeId: NodeId?, channel: Channel?) : AbstractRpcMessage<RequestVoteRpc?>(rpc, sourceNodeId!!, channel!!)
