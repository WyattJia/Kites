package raft.log

/**
 * Thrown when failed to operate log.
 */
open class LogException : RuntimeException {
    /**
     * Create.
     */
    constructor() {}

    /**
     * Create.
     *
     * @param message message
     */
    constructor(message: String?) : super(message) {}

    /**
     * Create.
     *
     * @param cause cause
     */
    constructor(cause: Throwable?) : super(cause) {}

    /**
     * Create.
     *
     * @param message message
     * @param cause cause
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}