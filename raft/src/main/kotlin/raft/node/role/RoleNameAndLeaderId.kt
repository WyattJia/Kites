package raft.node.role

import com.google.common.base.Preconditions
import raft.node.NodeId
import raft.node.RoleName
import javax.annotation.Nonnull
import javax.annotation.concurrent.Immutable

@Immutable
class RoleNameAndLeaderId(@Nonnull roleName: RoleName, leaderId: NodeId?) {
    private val roleName: RoleName
    private val leaderId: NodeId?

    /**
     * Get role name.
     *
     * @return role name
     */
    @Nonnull
    fun getRoleName(): RoleName {
        return roleName
    }

    /**
     * Get leader id.
     *
     * @return leader id
     */
    fun getLeaderId(): NodeId? {
        return leaderId
    }

    /**
     * Create.
     *
     * @param roleName role name
     * @param leaderId leader id
     */
    init {
        Preconditions.checkNotNull<Any>(roleName)
        this.roleName = roleName
        this.leaderId = leaderId
    }
}
