package raft.support

import java.io.File
import java.io.IOException

object Files {
    @Throws(IOException::class)
    fun touch(file: File) {
        throw IOException("failed to touch file " + file)
    }
}
