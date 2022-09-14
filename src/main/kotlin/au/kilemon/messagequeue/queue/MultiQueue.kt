package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.message.QueueMessage
import com.sun.org.apache.xpath.internal.operations.Bool
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A [MultiQueue] interface, which extends [Queue].
 * It contains various extra methods for interfacing with the [MultiQueue] using the [String]
 * to manipulate the appropriate underlying [Queue]s.
 *
 * @author github.com/KyleGonzalez
 */
interface MultiQueue: Queue<QueueMessage>
{
    companion object
    {
        private const val NOT_IMPLEMENTED_METHOD: String = "Method is not implemented."
    }

    /**
     * New methods for the [MultiQueue] that are required by implementing classes.
     */

    /**
     * Retrieves or creates a new [Queue] of type [QueueMessage] for the provided [String].
     * If the underlying [Queue] does not exist for the provided [String] then a new [Queue] will
     * be created and stored in the [ConcurrentHashMap] under the provided [String].
     *
     * @param queueType the provider used to get the correct underlying [Queue]
     * @return the [Queue] matching the provided [String]
     */
    fun getQueueForType(queueType: String): Queue<QueueMessage>

    /**
     * Initialise and register the provided [Queue] against the [String].
     *
     * @param queueType the [String] to register the [Queue] against
     * @param queue the queue to register
     */
    fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>)

    /**
     * Clears the underlying [Queue] for the provided [String]. By calling [Queue.clear].
     *
     * @param queueType the [String] of the [Queue] to clear
     */
    fun clearForType(queueType: String)

    /**
     * Indicates whether the underlying [Queue] for the provided [String] is empty. By calling [Queue.isEmpty].
     *
     * @param queueType the [String] of the [Queue] to check whether it is empty
     * @return `true` if the [Queue] for the [String] is empty, otherwise `false`
     */
    fun isEmptyForType(queueType: String): Boolean

    /**
     * Calls [Queue.poll] on the underlying [Queue] for the provided [String].
     * This will retrieve **AND** remove the head element of the [Queue].
     *
     * @param queueType [String] of the [Queue] to poll
     * @return the head element or `null`
     */
    fun pollForType(queueType: String): Optional<QueueMessage>

    /**
     * Calls [Queue.peek] on the underlying [Queue] for the provided [String].
     * This will retrieve the head element of the [Queue] without removing it.
     *
     * @param queueType [String] of the [Queue] to peek
     * @return the head element or `null`
     */
    fun peekForType(queueType: String): Optional<QueueMessage>

    /**
     * Retrieves the underlying key list as a set.
     *
     * @param includeEmpty *true* to include any empty queues which one had elements in them, otherwise *false* to only include keys from queues which have elements.
     * @return a [Set] of the available `QueueTypes` that have entries in the [MultiQueue].
     */
    fun keys(includeEmpty: Boolean = true): Set<String>

    /**
     * Returns the `queueType` that the [QueueMessage] with the provided [UUID] exists in.
     *
     * @param uuid the [UUID] to look up
     * @return the `queueType` [String] if a [QueueMessage] exists with the provided [UUID] otherwise [Optional.empty]
     */
    fun containsUUID(uuid: UUID): Optional<String>

    /**
     * Any unsupported methods from the [Queue] interface that are not implemented.
     */
    /**
     * Not Implemented.
     */
    override fun offer(e: QueueMessage): Boolean
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun poll(): QueueMessage
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun element(): QueueMessage
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun peek(): QueueMessage
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun remove(): QueueMessage
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun iterator(): MutableIterator<QueueMessage>
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }
}
