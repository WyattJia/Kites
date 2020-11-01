package raft.log

import raft.support.Files
import java.io.File
import java.io.IOException

abstract class AbstractLogDir(val dir: File) : LogDir {
    override fun initialize() {
        if (!dir.exists() && !dir.mkdir()) {
            throw LogException("failed to create directory $dir")
        }
        try {
            Files.touch(entriesFile)
            Files.touch(entryOffsetIndexFile)
        } catch (e: IOException) {
            throw LogException("failed to create file", e)
        }
    }

    override fun exists(): Boolean {
        return dir.exists()
    }

    override val snapshotFile: File
        get() = File(dir, RootDir.FILE_NAME_SNAPSHOT)
    override val entriesFile: File
        get() = File(dir, RootDir.FILE_NAME_ENTRIES)
    override val entryOffsetIndexFile: File
        get() = File(dir, RootDir.FILE_NAME_ENTRY_OFFSET_INDEX)

    override fun get(): File {
        return dir
    }

    override fun renameTo(logDir: LogDir?): Boolean {
        if (logDir != null) {
            return dir.renameTo(logDir.get())
        }
        return false
    }
}
