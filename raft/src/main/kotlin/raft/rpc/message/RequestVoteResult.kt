package raft.rpc.message

import java.io.Serializable

class RequestVoteResult(private val term: Int, private val voteGranted: Boolean) : Serializable {
    init {
        term
        voteGranted
    }

    fun getTerm(): Int {
        return term
    }

    fun isVoteGranted(): Boolean {
        return voteGranted
    }

    override fun toString(): String {
        return "RequestVoteResult{" + "term=" + term +
                ", voteGranted=" + voteGranted +
                '}'
    }
}