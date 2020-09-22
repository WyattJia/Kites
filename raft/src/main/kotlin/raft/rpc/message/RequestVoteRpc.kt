package raft.rpc.message

import raft.node.NodeId
import kotlin.properties.Delegates

class RequestVoteRpc {
    var term by Delegates.notNull<Int>()
    lateinit var candidateId: NodeId
    var lastLogIndex: Int = 0
    var lastLogTerm: Int = 0

    override fun toString(): String {
        return "RequestVoteRpc{" +
                "candidateId=" + candidateId +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", term=" + term +
                '}'
    }

}