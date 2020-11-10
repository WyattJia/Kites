package raft.log.statemachine

interface StateMachineContext {
    fun generateSnapshot(lastIncludedIndex: Int)

}