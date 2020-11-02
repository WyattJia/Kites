package raft.node.NodeGroup

import raft.node.GroupMember.GroupMember
import raft.node.NodeEndpoint.NodeEndpoint
import raft.node.NodeId
import raft.node.ReplicatingState.ReplicatingState
import raft.support.Log
import java.util.*
import java.util.stream.Collectors
import javax.annotation.Nonnull
import javax.annotation.concurrent.NotThreadSafe
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@NotThreadSafe
class NodeGroup(endpoints: Collection<NodeEndpoint>, selfId: NodeId) {
    private val selfId: NodeId
    private var memberMap: MutableMap<NodeId, GroupMember>

    companion object : Log {}

    /**
     * Create group with single member(standalone).
     *
     * @param endpoint endpoint
     */
    constructor(endpoint: NodeEndpoint) : this(setOf<NodeEndpoint>(endpoint), endpoint.id) {}

    /**
     * Build member map from endpoints.
     *
     * @param endpoints endpoints
     * @return member map
     * @throws IllegalArgumentException if endpoints is empty
     */
    private fun buildMemberMap(endpoints: Collection<NodeEndpoint>): MutableMap<NodeId, GroupMember> {
        val map: MutableMap<NodeId, GroupMember> = HashMap()
        for (endpoint in endpoints) {
            map[endpoint.id] = GroupMember(endpoint)
        }
        require(!map.isEmpty()) { "endpoints is empty" }
        return map
    }

    /**
     * Get count of major.
     *
     * For election.
     *
     * @return count
     * @see GroupMember.isMajor
     */
    val countOfMajor: Int
        get() = memberMap.values.stream().filter(GroupMember::isMajor).count().toInt()

    /**
     * Find self.
     *
     * @return self
     */
    @Nonnull
    fun findSelf(): GroupMember {
        return findMember(selfId)
    }

    /**
     * Find member by id.
     *
     * Throw exception if member not found.
     *
     * @param id id
     * @return member, never be `null`
     * @throws IllegalArgumentException if member not found
     */
    @Nonnull
    fun findMember(id: NodeId): GroupMember {
        return getMember(id) ?: throw IllegalArgumentException("no such node $id")
    }

    /**
     * Get member by id.
     *
     * @param id id
     * @return member, maybe `null`
     */
    fun getMember(id: NodeId): GroupMember? {
        return memberMap[id]
    }

    /**
     * Check if node is major member.
     *
     * @param id id
     * @return true if member exists and member is major, otherwise false
     */
    fun isMemberOfMajor(id: NodeId): Boolean {
        val member = memberMap[id]
        return member != null && member.isMajor
    }

    /**
     * Upgrade member to major member.
     *
     * @param id id
     * @throws IllegalArgumentException if member not found
     * @see .findMember
     */
    fun upgrade(id: NodeId) {
        logger().info("upgrade node $id")
        findMember(id).isMajor = true
    }

    /**
     * Downgrade member(set major to `false`).
     *
     * @param id id
     * @throws IllegalArgumentException if member not found
     */
    fun downgrade(id: NodeId) {
        logger().info("downgrade node $id")
        val member = findMember(id)
        member.isMajor = false
        member.setRemoving()
    }

    /**
     * Remove member.
     *
     * @param id id
     */
    fun removeNode(id: NodeId) {
        logger().info("node $id removed")
        memberMap.remove(id)
    }

    /**
     * Reset replicating state.
     *
     * @param nextLogIndex next log index
     */
    fun resetReplicatingStates(nextLogIndex: Int) {
        for (member in memberMap.values) {
            if (!member.idEquals(selfId)) {
                member.setReplicatingState(ReplicatingState(nextLogIndex))
            }
        }
    }

    val matchIndexOfMajor: Int
        get() {
            val matchIndices: MutableList<NodeMatchIndex> = ArrayList()
            for (member in memberMap.values) {
                if (member.isMajor && !member.idEquals(selfId)) {
                    matchIndices.add(NodeMatchIndex(member.id, member.matchIndex))
                }
            }
            val count = matchIndices.size
            check(count != 0) { "standalone or no major node" }
            Collections.sort(matchIndices)
            logger().debug("match indices {}", matchIndices)
            return matchIndices[count / 2].matchIndex
        }

    /**
     * List replication target.
     *
     * Self is not replication target.
     *
     * @return replication targets.
     */
    fun listReplicationTarget(): MutableList<Any>? {
        return memberMap.values.stream().filter { m: GroupMember ->
            !m.idEquals(
                selfId
            )
        }.collect(Collectors.toList())
    }

    /**
     * Add member to group.
     *
     * @param endpoint   endpoint
     * @param nextIndex  next index
     * @param matchIndex match index
     * @param major      major
     * @return added member
     */
    fun addNode(endpoint: NodeEndpoint, nextIndex: Int, matchIndex: Int, major: Boolean): GroupMember {
        logger().info("add node ${endpoint.id} to group")
        val replicatingState = ReplicatingState(nextIndex, matchIndex)
        val member = GroupMember(endpoint, replicatingState, major)
        memberMap[endpoint.id] = member
        return member
    }

    /**
     * Update member list.
     *
     * All replicating state will be dropped.
     *
     * @param endpoints endpoints
     */
    fun updateNodes(endpoints: Set<NodeEndpoint>) {
        memberMap = buildMemberMap(endpoints)
        logger().info("group change changed -> $memberMap.keys")
    }

    /**
     * List endpoint of major members.
     *
     * @return endpoints
     */
    fun listEndpointOfMajor(): Set<NodeEndpoint> {
        val endpoints: MutableSet<NodeEndpoint> = HashSet()
        for (member in memberMap.values) {
            if (member.isMajor) {
                endpoints.add(member.endpoint)
            }
        }
        return endpoints
    }

    /**
     * List endpoint of major members except self.
     *
     * @return endpoints except self
     */
    fun listEndpointOfMajorExceptSelf(): Set<NodeEndpoint> {
        val endpoints: MutableSet<NodeEndpoint> = HashSet()
        for (member in memberMap.values) {
            if (member.isMajor && !member.idEquals(selfId)) {
                endpoints.add(member.endpoint)
            }
        }
        return endpoints
    }

    /**
     * Check if member is unique one in group, in other word, check if standalone mode.
     *
     * @return true if only one member and the id of member equals to specified id, otherwise false
     */
    fun isStandalone(): Boolean {
        return memberMap.size == 1 && memberMap.containsKey(selfId)
    }

    /**
     * Node match index.
     *
     * @see NodeGroup.getMatchIndexOfMajor
     */
    private class NodeMatchIndex internal constructor(private val nodeId: NodeId, val matchIndex: Int) :
            Comparable<NodeMatchIndex?> {

        override operator fun compareTo(other: NodeMatchIndex?): Int {
            if (other != null) {
                return -Integer.compare(other.matchIndex, matchIndex)
            } else {
                throw NullPointerException("Node match index is null.")
            }
        }

        override fun toString(): String {
            return "<$nodeId, $matchIndex>"
        }


    }


    /**
     * Create group.
     *
     * @param endpoints endpoints
     * @param selfId    self id
     */
    init {
        memberMap = buildMemberMap(endpoints)
        this.selfId = selfId
    }
}


