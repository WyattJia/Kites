package raft.log

import java.io.File

interface LogDir {
    fun initialize()
    fun exists(): Boolean
    val snapshotFile: File?
    val entriesFile: File?
    val entryOffsetIndexFile: File?

    fun get(): File?
    fun renameTo(logDir: LogDir?): Boolean
}
