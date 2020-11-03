package raft.log

import org.slf4j.LoggerFactory
import java.io.File


internal class RootDir(baseDir: File) {
    private val baseDir: File
    val logDirForGenerating: LogDir
        get() = getOrCreateNormalLogDir(DIR_NAME_GENERATING)
    val logDirForInstalling: LogDir
        get() = getOrCreateNormalLogDir(DIR_NAME_INSTALLING)

    private fun getOrCreateNormalLogDir(name: String): NormalLogDir {
        val logDir = NormalLogDir(File(baseDir, name))
        if (!logDir.exists()) {
            logDir.initialize()
        }
        return logDir
    }

    fun rename(dir: LogDir, lastIncludedIndex: Int): LogDir {
        val destDir = LogGeneration(baseDir, lastIncludedIndex)
        check(!destDir.exists()) { "failed to rename, dest dir $destDir exists" }
        logger.info("rename dir {} to {}", dir, destDir)
        check(dir.renameTo(destDir)) { "failed to rename $dir to $destDir" }
        return destDir
    }

    fun createFirstGeneration(): LogGeneration {
        val generation = LogGeneration(baseDir, 0)
        generation.initialize()
        return generation
    }

    val latestGeneration: LogGeneration?
        get() {
            val files = baseDir.listFiles() ?: return null
            var latest: LogGeneration? = null
            var fileName: String
            var generation: LogGeneration
            for (file in files) {
                if (!file.isDirectory) {
                    continue
                }
                fileName = file.name
                if (DIR_NAME_GENERATING == fileName || DIR_NAME_INSTALLING == fileName ||
                        !LogGeneration.isValidDirName(fileName)
                ) {
                    continue
                }
                generation = LogGeneration(file)
                if (latest == null || generation.compareTo(latest) > 0) {
                    latest = generation
                }
            }
            return latest
        }

    companion object {
        const val FILE_NAME_SNAPSHOT = "service.ss"
        const val FILE_NAME_ENTRIES = "entries.bin"
        const val FILE_NAME_ENTRY_OFFSET_INDEX = "entries.idx"
        private const val DIR_NAME_GENERATING = "generating"
        private const val DIR_NAME_INSTALLING = "installing"
        private val logger = LoggerFactory.getLogger(RootDir::class.java)
    }

    init {
        require(baseDir.exists()) { "dir $baseDir not exists" }
        this.baseDir = baseDir
    }
}

