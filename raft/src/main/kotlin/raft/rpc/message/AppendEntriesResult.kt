package raft.rpc.message

import java.io.Serializable

class AppendEntriesResult(val rpcMessageId: String, val term: Int, val isSuccess: Boolean) : Serializable {

    override fun toString(): String {
        return "AppendEntriesResult{" +
                "rpcMessageId='" + rpcMessageId + '\'' +
                ", success=" + isSuccess +
                ", term=" + term +
                '}'
    }
}

