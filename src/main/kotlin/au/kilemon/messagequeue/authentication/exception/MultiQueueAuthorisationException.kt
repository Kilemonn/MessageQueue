package au.kilemon.messagequeue.authentication.exception

import au.kilemon.messagequeue.authentication.RestrictionMode

/**
 * An authorisation exception used when the `MultiQueueAuthenticator` does not allow the caller to perform the
 * requested action on the requested sub-queue. Due to either due to a mismatch of the provided token and the sub-queue
 * that is being request OR in hybrid mode if a token is required but not provided.
 *
 * @author github.com/Kilemonn
 */
class MultiQueueAuthorisationException(subQueue: String, restrictionMode: RestrictionMode) : Exception(String.format(MESSAGE_FORMAT, subQueue, restrictionMode))
{
    companion object
    {
        const val MESSAGE_FORMAT = "Unable to access sub-queue [%s]. Using mode [%s]."
    }
}
