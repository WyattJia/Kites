package raft.node.role

import raft.node.NodeId
import raft.node.RoleName
import javax.annotation.concurrent.Immutable

/**
 * Role name and leader id.
 */
@Immutable
class RoleNameAndLeaderId(roleName: RoleName, leaderId: NodeId?) {
    /**
     * Get role name.
     *
     * @return role name
     */
    val roleName: RoleName = roleName

    /**
     * Get leader id.
     *
     * @return leader id
     */
    val leaderId: NodeId? = leaderId

}
