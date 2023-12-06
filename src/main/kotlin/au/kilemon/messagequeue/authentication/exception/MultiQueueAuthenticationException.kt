package au.kilemon.messagequeue.authentication.exception

/**
 * An authentication exception used when the provided token is invalid when an action is requested to be performed on a
 * sub-queue.
 *
 * @author github.com/Kilemonn
 */
class MultiQueueAuthenticationException : Exception(ERROR_MESSAGE)
{
    companion object
    {
        const val ERROR_MESSAGE = "Invalid token provided."
    }
}
