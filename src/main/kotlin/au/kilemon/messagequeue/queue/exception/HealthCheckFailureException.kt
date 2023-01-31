package au.kilemon.messagequeue.queue.exception

/**
 * A specific exception used to indicate that the health check attempt failed for a specific reason.
 *
 * @author github.com/KyleGonzalez
 */
class HealthCheckFailureException(override val message: String, override val cause: Throwable? = null): Exception(message, cause)
