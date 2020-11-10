package raft.service

import org.slf4j.LoggerFactory
import raft.node.NodeId
import java.util.*
import kotlin.collections.set


class ServerRouter {
    private val availableServers: MutableMap<NodeId, Channel> = java.util.HashMap<NodeId, Channel>()
    private var leaderId: NodeId? = null
    fun send(payload: Any): Any? {
        for (nodeId in candidateNodeIds) {
            try {
                val result = doSend(nodeId as NodeId, payload)
                leaderId = nodeId
                return result
            } catch (e: RedirectException) {
                logger.debug("not a leader server, redirect to server {}", e.getLeaderId())
                leaderId = e.getLeaderId()
                return doSend(e.getLeaderId(), payload)
            } catch (e: Exception) {
                logger.debug("failed to process with server " + nodeId + ", cause " + e.message)
            }
        }
        throw NoAvailableServerException("no available server")
    }

    private val candidateNodeIds: Collection<Any>
        private get() {
            if (availableServers.isEmpty()) {
                throw NoAvailableServerException("no available server")
            }
            if (leaderId != null) {
                val nodeIds: MutableList<NodeId> = ArrayList<NodeId>()
                nodeIds.add(leaderId!!)
                for (nodeId in availableServers.keys) {
                    if (!nodeId.equals(leaderId)) {
                        nodeIds.add(nodeId)
                    }
                }
                return nodeIds
            }
            return availableServers.keys
        }

    private fun doSend(id: NodeId, payload: Any): Any? {
        val channel = availableServers[id] ?: throw IllegalStateException("no such channel to server $id")
        logger.debug("send request to server {}", id)
        return channel.send(payload)
    }

    fun add(id: NodeId, channel: Channel) {
        availableServers[id] = channel
    }

    fun getLeaderId(): NodeId? {
        return leaderId
    }

    fun setLeaderId(leaderId: NodeId) {
        check(availableServers.containsKey(leaderId)) { "no such server [$leaderId] in list" }
        this.leaderId = leaderId
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServerRouter::class.java)
    }
}

