package raft.log.snapshot

import raft.rpc.message.InstallSnapshotRpc

class NullSnapshotBuilder : SnapshotBuilder<Snapshot?> {
    override fun append(rpc: InstallSnapshotRpc?) {
        throw UnsupportedOperationException()
    }

    override fun build(): Snapshot {
        throw UnsupportedOperationException()
    }

    override fun close() {}
}

