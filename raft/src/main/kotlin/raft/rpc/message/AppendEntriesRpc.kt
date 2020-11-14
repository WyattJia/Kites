package raft.rpc.message

import raft.log.entry.Entry
import raft.node.NodeId
import java.io.Serializable


class AppendEntriesRpc : Serializable {
    var messageId: String? = null
    var term = 0
    var leaderId: NodeId? = null
    var prevLogIndex = 0
    var prevLogTerm = 0
    var entries = emptyList<Entry>()
    var leaderCommit = 0
    val lastEntryIndex: Int
        get() = if (entries.isEmpty()) prevLogIndex else entries[entries.size - 1].index

    override fun toString(): String {
        return "AppendEntriesRpc{" +
                "messageId='" + messageId +
                "', entries.size=" + entries.size +
                ", leaderCommit=" + leaderCommit +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", term=" + term +
                '}'
    }
}

