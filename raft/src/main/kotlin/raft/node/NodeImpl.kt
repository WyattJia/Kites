package raft.node

import FollowerNodeRole
import raft.support.Log
import kotlin.properties.Delegates

abstract class NodeImpl (private val context: NodeContext):Node{

    // core module context
    companion object: Log {}
    var started by Delegates.notNull<Boolean>()

    // current node role
    lateinit var role: AbstractNodeRole

    @Synchronized
    override fun start() {
        if (this.started) {
            return
        }

        context.eventbus.register(this)
        context.connector.initialize()

        var store = context.store()
        changeToRole(FollowerNodeRole(
            store.getTerm(),
            store.getVotedFor(),
            null,
            scheduleElectionTimeout()
            ))


        started = true

    }

    fun changeToRole(newRole: FollowerNodeRole) {

    }
}