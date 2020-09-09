package raft.support

class EventloopExecutor:TaskExecutor {
    override suspend fun submit() {
        launch {
            TODO("Not yet implemented")
        }
    }

    override suspend fun shutdown() {
        launch {
            TODO("Not yet implemented")
        }
    }
}