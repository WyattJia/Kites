package raft.node

import raft.node.role.RoleState


/**
 * Node role listener.
 */
interface NodeRoleListener {
    /**
     * Called when node role changes. e.g FOLLOWER to CANDIDATE.
     *
     * @param roleState role state
     */
    fun nodeRoleChanged(roleState: RoleState?)
}

