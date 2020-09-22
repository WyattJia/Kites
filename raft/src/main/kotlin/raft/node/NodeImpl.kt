package raft.node

import raft.node.role.CandidateNodeRole
import raft.node.role.FollowerNodeRole
import raft.rpc.message.RequestVoteRpc
import raft.schedule.ElectionTimeout
import raft.support.Log
import kotlin.properties.Delegates

abstract class NodeImpl(private val context: NodeContext):Node{

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
        return context.scheduler.scheduleElectionTimeout(this::electionTimeout)
    }

    fun electionTimeout() {
        context.taskExecutor.submit(this::doProcessElectionTimeout)
    }

    fun doProcessElectionTimeout() {
        if (role.getName() === RoleName.LEADER) {
            logger().warn(
                "node {}, current role is leader, ignore election timeout",
                context.selfId()
            )
            return
        }

        // follower: start election
        // candidate: restart election
        val newTerm: Int = role.term + 1
        role.cancelTimeoutOrTask()

        logger().info("Start election.")
        changeToRole(CandidateNodeRole(newTerm, scheduleElectionTimeout()))

        // request vote
        val rpc = RequestVoteRpc()
        rpc.term = newTerm
        rpc.candidateId = context.selfId()
        rpc.lastLogIndex = 0
        rpc.lastLogTerm = 0

        context.connector.sendRequestVote(rpc, context.group().listEndpointOfMajorExceptSelf())
    }


    fun changeToRole(newRole: AbstractNodeRole) {
        if (!isStableBetween(role, newRole)) {
            logger().debug("Node {}, role state changed -> {}", context.selfId(), newRole)

            var state = newRole.state
            var store = context.store()

            if (store != null) {
                if (state != null) {
                    store.setTerm(state.term)
                }
            }

            if (state != null) {
                state.votedFor?.let { store?.setVotedFor(it) }
            }
        }
    }

    fun isStableBetween(before: AbstractNodeRole, after: AbstractNodeRole): Boolean {
        assert(after != null)
        return before != null && before.stateEquals(after)
    }
}