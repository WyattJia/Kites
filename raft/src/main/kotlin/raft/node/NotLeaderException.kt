package raft.node

import com.google.common.base.Preconditions


/**
 * Thrown when current node is not leader.
 */
class NotLeaderException(roleName: RoleName, leaderEndpoint: NodeEndpoint?) :
    RuntimeException("not leader") {
    /**
     * Get role name.
     *
     * @return role name
     */
    val roleName: RoleName

    /**
     * Get leader endpoint.
     *
     * @return leader endpoint
     */
    val leaderEndpoint: NodeEndpoint?

    /**
     * Create.
     *
     * @param roleName       role name
     * @param leaderEndpoint leader endpoint
     */
    init {
        Preconditions.checkNotNull<Any>(roleName)
        this.roleName = roleName
        this.leaderEndpoint = leaderEndpoint
    }
}

