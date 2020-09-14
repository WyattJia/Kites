package raft.support

import com.sun.org.slf4j.internal.LoggerFactory

interface Log {
    fun logger() = LoggerFactory.getLogger(this.javaClass)
}