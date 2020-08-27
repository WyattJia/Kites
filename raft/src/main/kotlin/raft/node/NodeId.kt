package raft.node.NodeId


import java.io.Serializable
import java.lang.IllegalArgumentException


class NodeId(id: String) {


    var value:String = " "

    init {
        value = " "
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return true
    }



    override fun toString(): String {
        return "NodeId()"
    }

}