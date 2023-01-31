package au.kilemon.messagequeue.queue.exception

/**
 * A specific exception that is thrown when a message cannot be updated. Specifically when the message to update does not exist or there is an error when updating the message in the storage mechanism.
 *
 * @author github.com/KyleGonzalez
 */
class MessageUpdateException(uuid: String) : Exception("Unable to update message with UUID [$uuid] as it either does not exist (and cannot be updated) or there was an underlying error in the storage mechanism.")
