package raft.node

enum class NodeMode {
    STANDALONE,
    STANDBY,
    GROUP_MEMBER
}
