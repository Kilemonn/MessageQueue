package au.kilemon.messagequeue.authentication.exception

/**
 * An authentication exception used when the `MultiQueueAuthenticator` does not allow the caller to perform the
 * requested action on the requested sub-queue.
 *
 * @author github.com/Kilemonn
 */
class MultiQueueAuthenticationException(subQueue: String) : Exception("Unable to access sub-queue [$subQueue].")
