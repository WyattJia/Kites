package raft.node.task

enum class GroupConfigChangeTaskResult {
    OK, TIMEOUT, REPLICATION_FAILED, ERROR
}
