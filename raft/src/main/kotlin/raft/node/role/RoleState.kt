package raft.node.role

import raft.node.NodeId

public interface RoleState {
    var VOTES_COUNT_NOT_SET: Int = -1

    fun getRoleName():RoleName!!
    fun getTerm():Int
    fun getVotesCount():Int
    fun getVotedFor():NodeId!!
    fun getLeaderId(): NodeId!!
}
