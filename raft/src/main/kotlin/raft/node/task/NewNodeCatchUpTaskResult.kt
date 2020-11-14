package raft.node.task

class NewNodeCatchUpTaskResult {
    enum class State {
        OK, TIMEOUT, REPLICATION_FAILED
    }

    val state: State
    val nextIndex: Int
    val matchIndex: Int

    constructor(state: State) {
        this.state = state
        nextIndex = 0
        matchIndex = 0
    }

    constructor(nextIndex: Int, matchIndex: Int) {
        state = State.OK
        this.nextIndex = nextIndex
        this.matchIndex = matchIndex
    }
}