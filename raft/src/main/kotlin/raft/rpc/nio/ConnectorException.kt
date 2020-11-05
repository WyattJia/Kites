package raft.rpc.nio


class ConnectorException(message: String?, cause: Throwable?) :
    RuntimeException(message, cause)