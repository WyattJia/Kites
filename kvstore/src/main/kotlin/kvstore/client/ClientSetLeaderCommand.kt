package kvstore.client

import raft.node.NodeId


class ClientSetLeaderCommand : Command {
    override val name: String
        get() = "client-set-leader"


    override fun execute(arguments: String?, context: CommandContext?) {
        if (arguments != null) {
            require(!arguments.isEmpty()) { "usage: $name <node-id>" }
        }
        val nodeId = arguments?.let { NodeId(it) }
        try {
            context!!.clientLeader = nodeId!!
            System.out.println(nodeId)
        } catch (e: IllegalStateException) {
            System.err.println(e.message)
        }
    }
}

