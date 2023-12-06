package au.kilemon.messagequeue.queue.exception

/**
 * A specific exception used to indicate that an exist `QueueMessage` with the same `UUID` already exists and the new `QueueMessage` can not be added to the queue.
 *
 * @author github.com/Kilemonn
 */
class DuplicateMessageException(uuid: String, subQueue: String) : Exception("Duplicate message with UUID [$uuid] exists in sub-queue [$subQueue].")
