package raft.node

import raft.node.NodeId.NodeId
import raft.node.role.RoleNameAndLeaderId
import raft.node.role.RoleState


abstract class AbstractNodeRole(private val name:RoleName, val term: Int) {


    open fun getNameAndLeaderId(selfId: NodeId?): RoleNameAndLeaderId? {
        return RoleNameAndLeaderId(name, getLeaderId(selfId))
    }

    abstract fun getLeaderId(selfId: NodeId?): NodeId?

    abstract fun cancelTimeoutOrTask()

    abstract val state: RoleState?

    public fun getName():RoleName{
        return name
    }

    open fun stateEquals(that: AbstractNodeRole): Boolean {
        return if (name !== that.name || term != that.term) {
            false
        } else doStateEquals(that)
    }

    protected abstract fun doStateEquals(role: AbstractNodeRole?): Boolean

}

