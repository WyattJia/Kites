package raft.support

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Log {
    fun logger(): Logger = LoggerFactory.getLogger(this.javaClass)
}