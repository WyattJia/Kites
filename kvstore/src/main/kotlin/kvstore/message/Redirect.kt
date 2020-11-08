package kvstore.message

import raft.node.NodeId

/*
*  Todo: redirect node by network address.(Use DNS or consistent hashing.)
*/
class Redirect(val leaderId: String?) {

    constructor(leaderId: NodeId?) : this(leaderId?.getValue()) {}

    override fun toString(): String {
        return "Redirect{leaderId=$leaderId}"
    }
}