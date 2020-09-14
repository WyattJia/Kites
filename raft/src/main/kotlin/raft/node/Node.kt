package raft.node

interface Node {
    fun start()

    @Throws(InterruptedException::class)
    fun stop()
}