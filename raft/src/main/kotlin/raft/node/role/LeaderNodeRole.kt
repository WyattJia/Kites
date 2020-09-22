package raft.node.role

import raft.node.AbstractNodeRole
import raft.node.NodeId
import raft.node.RoleName
import raft.schedule.LogReplicationTask
import javax.annotation.concurrent.Immutable

@Immutable
class LeaderNodeRole(term: Int, logReplicationTask: LogReplicationTask) :
    AbstractNodeRole(RoleName.LEADER, term) {
    private val logReplicationTask: LogReplicationTask = logReplicationTask

    override fun getLeaderId(selfId: NodeId?): NodeId? {
        return selfId
    }


    override fun cancelTimeoutOrTask() {
        logReplicationTask.cancel()
    }

    override val state: RoleState
        get() = DefaultRoleState(RoleName.LEADER, term)

    override fun doStateEquals(role: AbstractNodeRole?): Boolean {
        return true
    }

    override fun toString(): String {
        return "LeaderNodeRole{term=$term, logReplicationTask=$logReplicationTask}"
    }

}

