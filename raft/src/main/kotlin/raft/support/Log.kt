package raft.support

import com.sun.org.slf4j.internal.Logger
import com.sun.org.slf4j.internal.LoggerFactory

interface Log {
    fun logger(): Logger = LoggerFactory.getLogger(this.javaClass)
}