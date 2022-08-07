package au.kilemon.messagequeue.message

import java.io.Serializable
import java.util.*

/**
 * A [MultiQueue] interface, which extends [Queue].
 * It contains various extra methods for interfacing with the [MultiQueue] using the [MessageType] and [Message]
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
     * Clears the underlying [Queue] for the provided [MessageType]. By calling [Queue.clear].
     *
     * @param messageType the [MessageType] of the [Queue] to clear
     */
    fun clearForType(messageType: MessageType)

    /**
     * Indicates whether the underlying [Queue] for the provided [MessageType] is empty. By calling [Queue.isEmpty].
     *
     * @param messageType the [MessageType] of the [Queue] to check whether it is empty
     * @return `true` if the [Queue] for the [MessageType] is empty, otherwise `false`
     */
    fun isEmptyForType(messageType: MessageType): Boolean

    /**
     * Calls [Queue.poll] on the underlying [Queue] for the provided [MessageType].
     * This will retrieve **AND** remove the head element of the [Queue].
     *
     * @param messageType [MessageType] of the [Queue] to poll
     * @return the head element or `null`
     */
    fun pollForType(messageType: MessageType): T?

    /**
     * Calls [Queue.peek] on the underlying [Queue] for the provided [MessageType].
     * This will retrieve the head element of the [Queue] without removing it.
     *
     * @param messageType [MessageType] of the [Queue] to peek
     * @return the head element or `null`
     */
    fun peekForType(messageType: MessageType): T?

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
