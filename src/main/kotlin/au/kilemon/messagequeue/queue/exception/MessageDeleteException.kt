package au.kilemon.messagequeue.queue.exception

/**
 * A specific exception that is thrown when a message cannot be deleted. Specifically when the message cannot be removed from the storage mechanism.
 *
 * @author github.com/Kilemonn
 */
class MessageDeleteException(uuid: String, exception: Exception? = null) : Exception("Unable to delete message with UUID [$uuid] as there was an underlying error in the storage mechanism.", exception)
