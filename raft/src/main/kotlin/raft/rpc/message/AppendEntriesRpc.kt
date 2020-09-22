package raft.rpc.message

import raft.log.entry.Entry
import raft.node.NodeId
import java.io.Serializable
import kotlin.properties.Delegates

class AppendEntriesRpc:Serializable {
    var term by Delegates.notNull<Int>()
    lateinit var leaderId: NodeId
    var prevLogIndex:Int = 0
    var prevLogTerm by Delegates.notNull<Int>()
    val entries = listOf<Entry>()
    var leaderCommit by Delegates.notNull<Int>()

    override fun toString(): String {
        return "AppendEntriesRpc{" +
                "', entries.size=" + entries.size +
                ", leaderCommit=" + leaderCommit +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", term=" + term +
                '}'
    }
}
