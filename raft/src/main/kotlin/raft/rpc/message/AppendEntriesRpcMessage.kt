package raft.rpc.message

import raft.node.NodeId
import raft.rpc.Channel


class AppendEntriesRpcMessage(rpc: AppendEntriesRpc, sourceNodeId: NodeId, channel: Channel?) : AbstractRpcMessage<AppendEntriesRpc?>(rpc, sourceNodeId, channel)

