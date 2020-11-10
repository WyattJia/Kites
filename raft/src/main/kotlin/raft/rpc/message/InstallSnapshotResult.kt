package raft.rpc.message


class InstallSnapshotResult(val term: Int) {

    override fun toString(): String {
        return "InstallSnapshotResult{" +
                "term=" + term +
                '}'
    }
}

