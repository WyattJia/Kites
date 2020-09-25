package raft.rpc.message

class AppendEntriesResult(private val term: Int, private val success: Boolean) {
    init {
        term
        success
    }

    fun getTerm(): Int {
        return term
    }

    fun isSuccess(): Boolean {
        return success
    }

    override fun toString(): String {
        return "AppendEntriesResult{" +
                ", success=" + success +
                ", term=" + term +
                '}'
    }
}