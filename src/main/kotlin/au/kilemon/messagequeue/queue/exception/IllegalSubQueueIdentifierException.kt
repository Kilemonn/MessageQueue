package au.kilemon.messagequeue.queue.exception

/**
 * A specific exception used to indicate that an invalid or reserved sub-queue identifier cannot be used.
 *
 * @author github.com/Kilemonn
 */
class IllegalSubQueueIdentifierException(subQueue: String) : Exception("Cannot access sub-queue with identifier [$subQueue] since it is reserved or invalid.")
