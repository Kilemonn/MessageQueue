package au.kilemon.messagequeue.authentication.exception

/**
 * An authentication exception used when the `MultiQueueAuthenticator` does not allow the caller to perform the
 * requested action on the requested sub-queue.
 *
 * @author github.com/Kilemonn
 */
class MultiQueueAuthenticationException(subQueue: String) : Exception(String.format(MESSAGE_FORMAT, subQueue))
{
    companion object
    {
        const val MESSAGE_FORMAT = "Unable to access sub-queue [%s]."
    }
}