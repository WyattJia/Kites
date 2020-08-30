package raft.node

import raft.node.NodeId

abstract class AbstractNodeRole(val name: RoleName, val term: Int) {

    fun getNameAndLeaderId(self: NodeId?): NodeId? {
        return RoleNameAndLeaderId(name, getLeaderId(selfId))
    }

    abstract fun cancelTimeoutOrTask()
    abstract fun getLeaderId(selfId: NodeId): NodeId?
    abstract val state: RoleState

    fun stateEquals(that: AbstractNodeRole): Boolean {
        return if (name != that.name || term != that.term) {
            false
        } else doStateEquals(that)
    }

    protected abstract fun doStateEquals(role: AbstractNodeRole): Boolean
}



