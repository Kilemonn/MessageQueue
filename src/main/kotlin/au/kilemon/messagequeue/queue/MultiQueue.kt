package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.queue.type.QueueTypeProvider
import java.io.Serializable
import java.util.*

/**
 * A [MultiQueue] interface, which extends [Queue].
 * It contains various extra methods for interfacing with the [MultiQueue] using the [QueueType] and [QueueMessage]
 * to manipulate the appropriate underlying [Queue]s.
 *
 * @author github.com/KyleGonzalez
 */
interface MultiQueue<T: Serializable>: Queue<T>
{
    companion object
    {
        private const val NOT_IMPLEMENTED_METHOD: String = "Method is not implemented."
    }

    /**
     * New methods for the [MultiQueue] that are required by implementing classes.
     */

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
    fun pollForType(queueTypeProvider: QueueTypeProvider): T?

    /**
     * Calls [Queue.peek] on the underlying [Queue] for the provided [QueueTypeProvider].
     * This will retrieve the head element of the [Queue] without removing it.
     *
     * @param queueTypeProvider [QueueTypeProvider] of the [Queue] to peek
     * @return the head element or `null`
     */
    fun peekForType(queueTypeProvider: QueueTypeProvider): T?

    /**
     * Any unsupported methods from the [Queue] interface that are not implemented.
     */
    /**
     * Not Implemented.
     */
    override fun offer(e: T): Boolean
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun poll(): T
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun element(): T
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun peek(): T
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun remove(): T
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }

    /**
     * Not Implemented.
     */
    override fun iterator(): MutableIterator<T>
    {
        throw UnsupportedOperationException(NOT_IMPLEMENTED_METHOD)
    }
}
