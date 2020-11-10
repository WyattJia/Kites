package raft.service

interface Channel {
    fun send(payload: Any?): Any?
}

