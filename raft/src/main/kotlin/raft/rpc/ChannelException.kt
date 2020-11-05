package raft.rpc

open class ChannelException : RuntimeException {
    constructor(cause: Throwable?) : super(cause) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}
