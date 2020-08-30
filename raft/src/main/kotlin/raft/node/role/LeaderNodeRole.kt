
class LeaderNodeRole(val logReplicationTask: LogReplicationTask):AbstractNodeRole(name, term) {
    init {
        super(RoleName.LEADER, term)
        this.logReplicationTask = logReplicationTask
    }

    public override fun cancelTimeoutOrTask() {
        logReplicationTask.cancel()
    }

    override fun toString(): String {
        return "LeaderNodeRole{term=" + term +
                ", logReplicationTasl=" + logReplicationTask +
                "}"
    }

}