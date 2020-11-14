package raft.node.task

class FixedResultGroupConfigTaskReference(
    result: GroupConfigChangeTaskResult,
) : GroupConfigChangeTaskReference {
    override val result: GroupConfigChangeTaskResult = result

    override fun getResult(timeout: Long): GroupConfigChangeTaskResult? {
        return result
    }

    override fun cancel() {}

}