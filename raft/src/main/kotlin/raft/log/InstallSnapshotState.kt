package raft.log

import raft.node.NodeEndpoint


class InstallSnapshotState {
    enum class StateName {
        ILLEGAL_INSTALL_SNAPSHOT_RPC, INSTALLING, INSTALLED
    }

    val stateName: StateName
    private var lastConfig: Set<NodeEndpoint>? = null

    constructor(stateName: StateName) {
        this.stateName = stateName
    }

    constructor(stateName: StateName, lastConfig: Set<NodeEndpoint>?) {
        this.stateName = stateName
        this.lastConfig = lastConfig
    }

    fun getLastConfig(): Set<NodeEndpoint>? {
        return lastConfig
    }
}

