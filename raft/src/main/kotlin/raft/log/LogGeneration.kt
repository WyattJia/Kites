package raft.log

import java.io.File
import java.util.regex.Pattern


class LogGeneration : AbstractLogDir, Comparable<LogGeneration?> {
    val lastIncludedIndex: Int

    constructor(baseDir: File?, lastIncludedIndex: Int) : super(File(baseDir, generateDirName(lastIncludedIndex))) {
        this.lastIncludedIndex = lastIncludedIndex
    }

    constructor(dir: File) : super(dir) {
        val matcher = DIR_NAME_PATTERN.matcher(dir.name)
        require(matcher.matches()) { "not a directory name of log generation, [" + dir.name + "]" }
        lastIncludedIndex = matcher.group(1).toInt()
    }

    override operator fun compareTo(other: LogGeneration?): Int {
        if (other != null) {
            return Integer.compare(lastIncludedIndex, other.lastIncludedIndex)
        }
        return 0
    }

    override fun toString(): String {
        return "LogGeneration{" +
                "dir=" + dir +
                ", lastIncludedIndex=" + lastIncludedIndex +
                '}'
    }

    companion object {
        private val DIR_NAME_PATTERN = Pattern.compile("log-(\\d+)")
        fun isValidDirName(dirName: String?): Boolean {
            return DIR_NAME_PATTERN.matcher(dirName).matches()
        }

        private fun generateDirName(lastIncludedIndex: Int): String {
            return "log-$lastIncludedIndex"
        }
    }

}

