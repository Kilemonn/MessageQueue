package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.exception.DuplicateMessageException
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.Throws

/**
 * A [MultiQueue] interface, which extends [Queue].
 * It contains various extra methods for interfacing with the [MultiQueue] using the [String]
 * to manipulate the appropriate underlying [Queue]s.
 *
 * @author github.com/KyleGonzalez
 */
interface MultiQueue: Queue<QueueMessage>, HasLogger
{
    companion object
    {
        private const val NOT_IMPLEMENTED_METHOD: String = "Method is not implemented."
    }

    override val LOG: Logger

    override var size: Int

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
     * This method should update the [size] property as part of the clearing of the sub-queue.
     *
     * @param queueType the [String] of the [Queue] to clear
     * @return the number of entries removed
     */
    fun clearForType(queueType: String): Int

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
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        val head = queueForType.poll()
        if (head != null)
        {
            LOG.debug("Found and removed head element with UUID [{}] from queue with type [{}].", head.uuid, queueType)
            size--
        }
        else
        {
            LOG.debug("No head element found when polling queue with type [{}].", queueType)
        }
        return Optional.ofNullable(head)
    }

    /**
     * Calls [Queue.peek] on the underlying [Queue] for the provided [String].
     * This will retrieve the head element of the [Queue] without removing it.
     *
     * @param queueType [String] of the [Queue] to peek
     * @return the head element or `null`
     */
    fun peekForType(queueType: String): Optional<QueueMessage>
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        val peeked = queueForType.peek()
        if (peeked != null)
        {
            LOG.debug("Found head element with UUID [{}] from queue with type [{}].", peeked.uuid, queueType)
        }
        else
        {
            LOG.debug("No head element found when peeking queue with type [{}].", queueType)
        }
        return Optional.ofNullable(peeked)
    }

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
     * @param uuid the [UUID] (as a [String]) to look up
     * @return the `queueType` [String] if a [QueueMessage] exists with the provided [UUID] otherwise [Optional.empty]
     */
    fun containsUUID(uuid: String): Optional<String>

    /**
     * Any overridden methods to update the signature for all implementing [MultiQueue] classes.
     */
    /**
     * Override [add] method to declare [Throws] [DuplicateMessageException] annotation.
     *
     * @throws [DuplicateMessageException] if a message already exists with the same [QueueMessage.uuid] in `any` other queue.
     */
    @Throws(DuplicateMessageException::class)
    override fun add(element: QueueMessage): Boolean
    {
        val elementIsMappedToType = containsUUID(element.uuid.toString())
        if ( !elementIsMappedToType.isPresent)
        {
            val wasAdded = performAdd(element)
            return if (wasAdded)
            {
                size++
                LOG.debug("Added new message with uuid [{}] to queue with type [{}].", element.uuid, element.type)
                true
            }
            else
            {
                LOG.error("Failed to add message with uuid [{}] to queue with type [{}].", element.uuid, element.type)
                false
            }
        }
        else
        {
            val existingQueueType = elementIsMappedToType.get()
            LOG.warn("Did not add new message with uuid [{}] to queue with type [{}] as it already exists in queue with type [{}].", element.uuid, element.type, existingQueueType)
            throw DuplicateMessageException(element.uuid.toString(), existingQueueType)
        }
    }

    /**
     * The internal add method to be called.
     * This is not to  be called directly.
     *
     * @param element the element to add
     * @return `true` if the element was added successfully, otherwise `false`.
     */
    fun performAdd(element: QueueMessage): Boolean

    override fun remove(element: QueueMessage): Boolean
    {
        val wasRemoved = performRemove(element)
        if (wasRemoved)
        {
            size--
            LOG.debug("Removed element with UUID [{}] from queue with type [{}].", element.uuid, element.type)
        }
        else
        {
            LOG.error("Failed to remove element with UUID [{}] from queue with type [{}].", element.uuid, element.type)
        }
        return wasRemoved
    }

    /**
     * The internal remove method to be called.
     * This is not to be called directly.
     *
     * @param element the element to remove
     * @return `true` if the element was removed successfully, otherwise `false`.
     */
    fun performRemove(element: QueueMessage): Boolean

    override fun contains(element: QueueMessage?): Boolean
    {
        if (element == null)
        {
            return false
        }
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        return queueForType.contains(element)
    }

    override fun containsAll(elements: Collection<QueueMessage>): Boolean
    {
        return elements.stream().allMatch{ element -> this.contains(element) }
    }

    override fun addAll(elements: Collection<QueueMessage>): Boolean
    {
        var allAdded = true
        for (element: QueueMessage in elements)
        {
            allAdded = try {
                val wasAdded = add(element)
                allAdded && wasAdded
            }
            catch (ex: DuplicateMessageException)
            {
                false
            }
        }
        return allAdded
    }

    override fun retainAll(elements: Collection<QueueMessage>): Boolean
    {
        var anyWasRemoved = false
        for (key: String in keys(false))
        {
            // The queue should never be new or created since we passed `false` into `keys()` above.
            val queueForKey: Queue<QueueMessage> = getQueueForType(key)
            for(entry: QueueMessage in queueForKey)
            {
                if ( !elements.contains(entry))
                {
                    LOG.debug("Message with uuid [{}] does not exist in retain list, attempting to remove.", entry.uuid)
                    val wasRemoved = queueForKey.remove(entry)
                    anyWasRemoved = wasRemoved || anyWasRemoved
                    if (wasRemoved)
                    {
                        LOG.debug("Removed message with uuid [{}] as it does not exist in retain list.", entry.uuid)
                        size--
                    }
                    else
                    {
                        LOG.error("Failed to remove message with uuid [{}].", entry.uuid)
                    }
                }
                else
                {
                    LOG.debug("Retaining element with uuid [{}] as it exists in the retain list.", entry.uuid)
                }
            }
        }
        return anyWasRemoved
    }

    override fun removeAll(elements: Collection<QueueMessage>): Boolean
    {
        var wasRemoved = false
        for (element: QueueMessage in elements)
        {
            wasRemoved = remove(element) || wasRemoved
        }
        return wasRemoved
    }

    /**
     * @return `true` if the [size] is `0`, otherwise `false`.
     */
    override fun isEmpty(): Boolean
    {
        return size == 0
    }

    override fun clear()
    {
        val keys = keys()
        var removedEntryCount = 0
        for (key in keys)
        {
            val amountRemovedForQueue = clearForType(key)
            removedEntryCount += amountRemovedForQueue
        }
        LOG.debug("Cleared multi-queue, removed [{}] message entries over [{}] queue types.", removedEntryCount, keys)
    }

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
