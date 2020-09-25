package raft.node.store

class NodeStoreException : RuntimeException {
    /**
     * Create.
     *
     * @param cause cause
     */
    constructor(cause: Throwable) : super(cause) {}

    /**
     * Create.
     *
     * @param message message
     * @param cause cause
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {}
}
