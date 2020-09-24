package raft.node.config

import raft.log.Log


/**
 * Node configuration.
 *
 *
 * Node configuration should not change after initialization. e.g [NodeBuilder].
 *
 */
class NodeConfig {
    /**
     * Minimum election timeout
     */
    var minElectionTimeout = 3000

    /**
     * Maximum election timeout
     */
    var maxElectionTimeout = 4000

    /**
     * Delay for first log replication after becoming leader
     */
    var logReplicationDelay = 0

    /**
     * Interval for log replication task.
     * More specifically, interval for heartbeat rpc.
     * Append entries rpc may be sent less than this interval.
     * e.g after receiving append entries result from followers.
     */
    var logReplicationInterval = 1000

    /**
     * Read timeout to receive response from follower.
     * If no response received from follower, resend log replication rpc.
     */
    var logReplicationReadTimeout = 900

    /**
     * Max entries to send when replicate log to followers
     */
    var maxReplicationEntries: Int = Log.ALL_ENTRIES

    /**
     * Max entries to send when replicate log to new node
     */
    var maxReplicationEntriesForNewNode: Int = Log.ALL_ENTRIES

    /**
     * Data length in install snapshot rpc.
     */
    var snapshotDataLength = 1024

    /**
     * Worker thread count in nio connector.
     */
    var nioWorkerThreads = 0

    /**
     * Max round for new node to catch up.
     */
    var newNodeMaxRound = 10

    /**
     * Read timeout to receive response from new node.
     * Default to election timeout.
     */
    var newNodeReadTimeout = 3000

    /**
     * Timeout for new node to make progress.
     * If new node cannot make progress after this timeout, new node cannot be added and reply TIMEOUT.
     * Default to election timeout
     */
    var newNodeAdvanceTimeout = 3000

    /**
     * Timeout to wait for previous group config change to complete.
     * Default is `0`, forever.
     */
    var previousGroupConfigChangeTimeout = 0
}


