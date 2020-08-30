package raft.node.role

import raft.node.NodeId

class RoleNameAndLeaderId(val roleName:RoleName, val leaderId: NodeId) {
   init {
       Preconditions.checkNotNull(roleName)

       this.rolename = roleName!!
       this.leaderId = leaderId?
   }

    public fun getRoleName(): RoleName{
        return roleName
    }

    public fun getLeaderId():NodeId {
        return leaderId
    }

}