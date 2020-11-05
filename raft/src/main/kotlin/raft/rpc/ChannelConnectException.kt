package raft.rpc


class ChannelConnectException : ChannelException {
    constructor(cause: Throwable?) : super(cause) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}

