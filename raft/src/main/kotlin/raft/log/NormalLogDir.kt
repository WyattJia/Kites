package raft.log

import java.io.File


class NormalLogDir internal constructor(dir: File?) : AbstractLogDir(dir!!) {
    override fun toString(): String {
        return "NormalLogDir{" +
                "dir=" + dir +
                '}'
    }
}
