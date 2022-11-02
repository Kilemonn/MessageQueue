package au.kilemon.messagequeue.queue.exception

/**
 * A specific exception used to indicate that an exist `QueueMessage` with the same `UUID` already exists and the new `QueueMessage` can not be added to the queue.
 */
class DuplicateMessageException(uuid: String, queueType: String, ) : Exception("Duplicate message with UUID [$uuid] exists in queue with type [$queueType].")
