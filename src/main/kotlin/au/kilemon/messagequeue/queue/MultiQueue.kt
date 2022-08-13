package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.type.QueueTypeProvider
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A [MultiQueue] interface, which extends [Queue].
 * It contains various extra methods for interfacing with the [MultiQueue] using the [QueueTypeProvider]
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
     * Retrieves or creates a new [Queue] of type [QueueMessage] for the provided [QueueTypeProvider].
     * If the underlying [Queue] does not exist for the provided [QueueTypeProvider] then a new [Queue] will
     * be created and stored in the [ConcurrentHashMap] under the provided [QueueTypeProvider].
     *
     * @param queueTypeProvider the provider used to get the correct underlying [Queue]
     * @return the [Queue] matching the provided [QueueTypeProvider]
     */
    fun getQueueForType(queueTypeProvider: QueueTypeProvider): Queue<QueueMessage>

    /**
     * Initialise and register the provided [Queue] against the [QueueTypeProvider].
     *
     * @param queueTypeProvider the [QueueTypeProvider] to register the [Queue] against
     * @param queue the queue to register
     */
    fun initialiseQueueForType(queueTypeProvider: QueueTypeProvider, queue: Queue<QueueMessage>)

    /**
     * Clears the underlying [Queue] for the provided [QueueTypeProvider]. By calling [Queue.clear].
     *
     * @param queueTypeProvider the [QueueTypeProvider] of the [Queue] to clear
     */
    fun clearForType(queueTypeProvider: QueueTypeProvider)

    /**
     * Indicates whether the underlying [Queue] for the provided [QueueTypeProvider] is empty. By calling [Queue.isEmpty].
     *
     * @param queueTypeProvider the [QueueTypeProvider] of the [Queue] to check whether it is empty
     * @return `true` if the [Queue] for the [QueueTypeProvider] is empty, otherwise `false`
     */
    fun isEmptyForType(queueTypeProvider: QueueTypeProvider): Boolean

    /**
     * Calls [Queue.poll] on the underlying [Queue] for the provided [QueueTypeProvider].
     * This will retrieve **AND** remove the head element of the [Queue].
     *
     * @param queueTypeProvider [QueueTypeProvider] of the [Queue] to poll
     * @return the head element or `null`
     */
    fun pollForType(queueTypeProvider: QueueTypeProvider): QueueMessage?

    /**
     * Calls [Queue.peek] on the underlying [Queue] for the provided [QueueTypeProvider].
     * This will retrieve the head element of the [Queue] without removing it.
     *
     * @param queueTypeProvider [QueueTypeProvider] of the [Queue] to peek
     * @return the head element or `null`
     */
    fun peekForType(queueTypeProvider: QueueTypeProvider): QueueMessage?

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
