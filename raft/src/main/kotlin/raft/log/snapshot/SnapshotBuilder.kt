package raft.log.snapshot

import raft.rpc.message.InstallSnapshotRpc

interface SnapshotBuilder<T : Snapshot?> {
    fun append(rpc: InstallSnapshotRpc?)
    fun build(): T
    fun close()
}

