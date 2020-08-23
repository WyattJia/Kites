class ReplicatingState {
    var nextIndex: Int = 0
        get() = field
        set(value) {
            field = value
        }
    var matchIndex: Int = 0
        get() = field
        set(value) {
            field = value
        }
    var replicating: Boolean = false
    var lastReplicatedAt: Long = 0

    init {
        nextIndex = 0
    }

    constructor(nextIndex: Int, matchIndex: Int) {
        this.nextIndex = nextIndex
        this.matchIndex = matchIndex
    }

    fun backOffNextIndex(): Boolean {
        if (nextIndex > 1) {
            nextIndex --
            return true
        }
        return false
    }


    fun advance(lastEntryIndex: Int): Boolean {
        var result: Boolean = (matchIndex != lastEntryIndex || nextIndex != (lastEntryIndex + 1))

        matchIndex = lastEntryIndex
        nextIndex = lastEntryIndex + 1

        return result
    }

    fun isReplicating(): Boolean {
        return replicating
    }

    @JvmName("setReplicating1")
    fun setReplicating(replicating: Boolean){
        this.replicating = replicating
    }

    fun setLastReplicatedAt(): Long {
        return lastReplicatedAt
    }

    @JvmName("setLastReplicatedAt1")
    fun setLastReplicatedAt(lastReplicatedAt: Long){
        this.lastReplicatedAt = lastReplicatedAt
    }

    override fun toString(): String {
        return "ReplicatingState{" +
                "nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                ", replicating=" + replicating +
                ", lastReplicatedAt=" + lastReplicatedAt +
                "}"
    }

}
