package raft.node

import raft.node.role.FollowerNodeRole
import raft.node.role.RoleState
import raft.schedule.ElectionTimeout
import raft.support.Log
import java.util.prefs.NodeChangeListener
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
        if (store != null) {
            changeToRole(
                FollowerNodeRole(
                    store.getTerm(),
                    store.getVotedFor(),
                    null,
                    scheduleElectionTimeout()
                )
            )
        }


        started = true

    }

    private fun scheduleElectionTimeout():ElectionTimeout {
        return context.scheduler.scheduleElectionTimeout(this::ele)
    }

    fun electionTimeout() {
        context.taskExecutor.submit()
    }

    private fun doProcessElectionTimeout() {
        if (role.getName() == RoleName.LEADER) {
            logger().warn("Node ${context.selfId}, current role is leader, ignore election timeout")
            return
        }

        var newTerm: Int = role.term + 1
        role.cancelTimeoutOrTask()
//
//        if (context.group().isStandalone()) {
//            if (context.mode == NodeMode.)
//        }
    }

    fun changeToRole(newRole: AbstractNodeRole) {
        if (!isStableBetween(role, newRole)) {
            logger().debug("Node {}, role state changed -> {}", context.selfId(), newRole)

            var state = newRole.state
            var store = context.store()

            if (store != null) {
                if (state != null) {
                    store.setTerm(state.getTerm())
                }
            }

            if (state != null) {
                store?.setVotedFor(state.getVotedFor())
            }

            roleListeners
        }


    }

    fun isStableBetween(before: AbstractNodeRole, after: AbstractNodeRole): Boolean {
        assert(after != null)
        return before != null && before.stateEquals(after)
    }
}