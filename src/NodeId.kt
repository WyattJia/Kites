import java.io.Serializable
import java.lang.IllegalArgumentException


class NodeId() {


    var value:String = " "

    init {
        value = " "
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "NodeId()"
    }

}