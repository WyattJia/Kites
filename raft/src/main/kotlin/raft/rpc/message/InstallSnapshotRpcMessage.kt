package raft.rpc.message

import raft.node.NodeId
import raft.rpc.Channel


class InstallSnapshotRpcMessage(rpc: InstallSnapshotRpc?, sourceNodeId: NodeId?, channel: Channel?) :
    AbstractRpcMessage<InstallSnapshotRpc?>(rpc, sourceNodeId, channel)
