package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.exception.*
import lombok.Generated
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Collectors

/**
 * A [MultiQueue] base class, which extends [Queue].
 * It contains various extra methods for interfacing with the [MultiQueue] using the [String] as a sub-queue identifier
 * to manipulate the appropriate underlying [Queue]s.
 *
 * @author github.com/Kilemonn
 */
abstract class MultiQueue: Queue<QueueMessage>, HasLogger
{
    companion object
    {
        private const val NOT_IMPLEMENTED_METHOD: String = "Method is not implemented."
    }

    abstract override val LOG: Logger

    @Autowired
    @get:Generated
    @set:Generated
    protected lateinit var multiQueueAuthenticator: MultiQueueAuthenticator

    /**
     * Get the underlying size of the [MultiQueue].
     * This is done by summing the length of each [getSubQueue] for each key in [keys].
     *
     * This is to allow the underlying storage to be the source of truth instead of any temporary counters, since the underlying storage could
     * change at any timeout without direct interaction from the [MultiQueue].
     */
    override val size: Int
        get() {
            var internalSize = 0
            keys(false).forEach { key ->
                internalSize += getSubQueue(key).size
            }
            return internalSize
        }

    /**
     * Get the next queue index.
     * If it does not exist yet, a default value of 1 will be set and returned.
     *
     * This can be overridden to return [Optional.EMPTY] to not override the ID of the
     * incoming messages even if it is empty as it is maintained by the underlying mechanism
     * (in most cases a database).
     *
     * @return the current value of the index before it was incremented
     */
    abstract fun getNextSubQueueIndex(subQueue: String): Optional<Long>

    /**
     * A wrapper for the [MultiQueue.persistMessageInternal] so this method can be synchronised.
     *
     * Synchronising so that multiple messages are not updated out of order.
     *
     * @param message the updated [QueueMessage] to persist
     * @return `true` if the [QueueMessage] was updated successfully, otherwise `false`
     */
    @Synchronized
    @Throws(MessageUpdateException::class)
    fun persistMessage(message: QueueMessage)
    {
        persistMessageInternal(message)
        LOG.trace("Successfully persisted message [{}].", message.uuid)
    }

    /**
     * Used to persist the updated [QueueMessage] to the storage mechanism.
     * It must match an existing message this should not create a new [QueueMessage].
     *
     * @throws MessageUpdateException if the message to update does not exist or there is an error
     * in the underlying storage mechanism when performing the update.
     *
     * @param message the updated [QueueMessage] to persist
     * @return `true` if the [QueueMessage] was updated successfully, otherwise `false`
     */
    @Throws(MessageUpdateException::class)
    abstract fun persistMessageInternal(message: QueueMessage)

    /**
     * Retrieves or creates a new [Queue] of [QueueMessage] for the provided [subQueue].
     * If the underlying [Queue] does not exist for the provided [subQueue] then a new [Queue] will
     * be created.
     *
     * **This method should not be called directly, please use [getSubQueue]**
     *
     * @param subQueue the identifier of the sub-queue [Queue]
     * @return the [Queue] matching the provided [String]
     */
    abstract fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>

    /**
     * Retrieves or creates a new [Queue] of type [QueueMessage] for the provided [String].
     * If the underlying [Queue] does not exist for the provided [String] then a new [Queue] will
     * be created.
     *
     * @param subQueue the identifier of the sub-queue [Queue]
     * @return the [Queue] matching the provided [String]
     * @throws IllegalSubQueueIdentifierException If the provided [subQueue] is part of the [MultiQueueAuthenticator.getReservedSubQueues]
     */
    @Throws(IllegalSubQueueIdentifierException::class)
    fun getSubQueue(subQueue: String): Queue<QueueMessage>
    {
        if (!multiQueueAuthenticator.isInNoneMode() && multiQueueAuthenticator.getReservedSubQueues().contains(subQueue))
        {
            throw IllegalSubQueueIdentifierException(subQueue)
        }

        val queue = getSubQueueInternal(subQueue)
        LOG.trace("Retrieved subQueue [{}] with [{}] elements.", subQueue, queue.size)
        return queue
    }

    /**
     * Retrieves only assigned messages in the sub-queue for the provided [subQueue].
     *
     * By default, this calls [getSubQueue] and iterates through this to determine if the [QueueMessage.assignedTo]
     * field is `not-null`, if [assignedTo] is `null` or is equal to the provided [assignedTo] if it is `not-null`.
     *
     * @param subQueue the identifier of the sub-queue [Queue]
     * @param assignedTo to further filter the messages returned this can be provided
     * @return a limited version of the [Queue] containing only assigned messages
     */
    open fun getAssignedMessagesInSubQueue(subQueue: String, assignedTo: String?): Queue<QueueMessage>
    {
        val assignedMessages = ConcurrentLinkedQueue<QueueMessage>()
        val queue = getSubQueue(subQueue)
        if (assignedTo == null)
        {
            assignedMessages.addAll(queue.stream().filter { message -> message.assignedTo != null }.collect(Collectors.toList()))
        }
        else
        {
            assignedMessages.addAll(queue.stream().filter { message -> message.assignedTo == assignedTo }.collect(Collectors.toList()))
        }
        return assignedMessages
    }

    /**
     * Retrieves only unassigned messages in the sub-queue for the provided [subQueue].
     *
     * By default, this iterates over [getSubQueue] and includes only entries where [QueueMessage.assignedTo] is `null`.
     *
     * @param subQueue the identifier of the sub-queue [Queue]
     * @return a limited version of the [Queue] containing only unassigned messages
     */
    open fun getUnassignedMessagesInSubQueue(subQueue: String): Queue<QueueMessage>
    {
        val unassignedMessages = ConcurrentLinkedQueue<QueueMessage>()
        val queue = getSubQueue(subQueue)
        unassignedMessages.addAll(queue.stream().filter { message -> message.assignedTo == null }.collect(Collectors.toList()))
        LOG.trace("Retrieved [{}] unassigned messages in subQueue [{}].", unassignedMessages.size, subQueue)
        return unassignedMessages
    }

    /**
     * Get a map of assignee identifiers and the sub-queue identifier that they own messages in.
     * If the [subQueue] is provided this will iterate over all sub-queues and call [getOwnersAndKeysMapForSubQueue] for each of them.
     * Otherwise, it will only call [getOwnersAndKeysMapForSubQueue] on the provided [subQueue] if it is not null.
     *
     * @param subQueue to retrieve the [Map] from
     * @return the [Map] of assignee identifiers mapped to a list of the sub-queue identifiers that they own any messages in
     */
    fun getOwnersAndKeysMap(subQueue: String?): Map<String, HashSet<String>>
    {
        val responseMap = HashMap<String, HashSet<String>>()
        val keyList = if (subQueue != null)
        {
            LOG.debug("Getting owners map for sub-queue with identifier [{}].", subQueue)
            setOf(subQueue)
        }
        else
        {
            val keys = keys(false)
            LOG.debug("Getting owners map for all [{}] sub-queues.", keys.size)
            keys
        }
        keyList.forEach { key -> getOwnersAndKeysMapForSubQueue(key, responseMap) }
        return responseMap
    }

    /**
     * Add an entry to the provided [Map] if any of the messages in the sub-queue are assigned.
     * The [QueueMessage.subQueue] is appended to the [Set] under it's [QueueMessage.assignedTo] identifier.
     *
     * @param subQueue the sub-queue to iterate and update the map from
     * @param responseMap the map to update
     */
    fun getOwnersAndKeysMapForSubQueue(subQueue: String, responseMap: HashMap<String, HashSet<String>>)
    {
        val queue: Queue<QueueMessage> = getAssignedMessagesInSubQueue(subQueue, null)
        queue.forEach { message ->
            val subQueueID = message.subQueue
            val assigned = message.assignedTo
            if (assigned != null)
            {
                LOG.trace("Appending sub-queue identifier [{}] to map for assignee ID [{}].", subQueueID, assigned)
                if (!responseMap.contains(assigned))
                {
                    val set = hashSetOf(subQueueID)
                    responseMap[assigned] = set
                }
                else
                {
                    // Set should not be null since we initialise and set it if the key is contained
                    responseMap[assigned]!!.add(subQueueID)
                }
            }
        }
    }

    /**
     * Performs a health check on the underlying storage mechanism.
     * If there are any errors a [HealthCheckFailureException] should be thrown, otherwise no exception should be thrown.
     */
    @Throws(HealthCheckFailureException::class)
    fun performHealthCheck()
    {
        try
        {
            performHealthCheckInternal()
            LOG.trace("Health check successful.")
        }
        catch (ex: Exception)
        {
            val errorMessage = "Health check failed on multi-queue."
            LOG.error(errorMessage, ex)
            throw HealthCheckFailureException(errorMessage, ex)
        }
    }

    /**
     * Performs a health check on the underlying storage mechanism.
     * If there are any errors an [Exception] should be thrown, otherwise no exception should be thrown to indicate a sucessful health check.
     */
    abstract fun performHealthCheckInternal()

    /**
     * Get a [QueueMessage] directly from the [MultiQueue] that matches the provided [uuid].
     *
     * @param uuid of the [QueueMessage] to find within the [MultiQueue]
     * @return the matching [QueueMessage] or [Optional.EMPTY]
     */
    abstract fun getMessageByUUID(uuid: String): Optional<QueueMessage>

    /**
     * Clears the underlying [Queue] for the provided [String]. By calling [Queue.clear].
     *
     * This method should update the [size] property as part of the clearing of the sub-queue.
     *
     * @param subQueue the [String] of the [Queue] to clear
     * @return the number of entries removed
     */
    abstract fun clearSubQueueInternal(subQueue: String): Int

    /**
     * Call to [MultiQueue.clearSubQueueInternal].
     *
     * @param subQueue the [String] of the [Queue] to clear
     * @return the number of entries removed
     */
    fun clearSubQueue(subQueue: String): Int
    {
        val removedEntries = clearSubQueueInternal(subQueue)
        LOG.trace("Cleared subQueue [{}], [{}] messages removed.", subQueue, removedEntries)
        return removedEntries
    }

    /**
     * Indicates whether the underlying [Queue] for the provided [String] is empty. By calling [Queue.isEmpty].
     *
     * @param subQueue the [String] of the [Queue] to check whether it is empty
     * @return `true` if the [Queue] for the [String] is empty, otherwise `false`
     */
    abstract fun isEmptySubQueue(subQueue: String): Boolean

    /**
     * Calls [Queue.poll] on the underlying [Queue] for the provided [String].
     * This will retrieve **AND** remove the head element of the [Queue].
     *
     * @param subQueue [String] of the [Queue] to poll
     * @return the head element or [Optional.EMPTY]
     */
    @Throws(MessageDeleteException::class)
    open fun pollSubQueue(subQueue: String): Optional<QueueMessage>
    {
        val head = pollInternal(subQueue)
        if (head.isPresent)
        {
            if (remove(head.get()))
            {
                LOG.debug("Found and removed head element with UUID [{}] from sub-queue [{}].", head.get().uuid, subQueue)
            }
            else
            {
                LOG.error("Attempted to poll subQueue [{}] and failed to remove head element.", subQueue)
            }
        }
        else
        {
            LOG.debug("No head element found when polling sub-queue [{}].", subQueue)
        }
        return head
    }

    /**
     * The internal poll method to be called.
     * This is not to  be called directly.
     *
     * This method should return the first element in the queue for the provided [subQueue].
     * *The caller will remove this element*.
     *
     * @param subQueue the sub-queue to poll
     * @return the first message wrapped as an [Optional] otherwise [Optional.empty]
     */
    abstract fun pollInternal(subQueue: String): Optional<QueueMessage>

    /**
     * Calls [Queue.peek] on the underlying [Queue] for the provided [String].
     * This will retrieve the head element of the [Queue] without removing it.
     *
     * @param subQueue [String] of the [Queue] to peek
     * @return the head element or `null`
     */
    fun peekSubQueue(subQueue: String): Optional<QueueMessage>
    {
        val queue: Queue<QueueMessage> = getSubQueue(subQueue)
        val peeked = Optional.ofNullable(queue.peek())
        if (peeked.isPresent)
        {
            LOG.debug("Found head element with UUID [{}] from sub-queue [{}].", peeked.get().uuid, subQueue)
        }
        else
        {
            LOG.debug("No head element found when peeking sub-queue [{}].", subQueue)
        }
        return peeked
    }

    /**
     * Retrieves the underlying key list as a set.
     * **Should be called directly, please using [keys].**
     *
     * @param includeEmpty *true* to include any empty queues which one had elements in them, otherwise *false* to only include keys from queues which have elements.
     * @return a [HashSet] of the available `Sub-Queues` that have entries in the [MultiQueue].
     */
    abstract fun keysInternal(includeEmpty: Boolean = true): HashSet<String>

    /**
     * Delegates to [keysInternal] and removes any keys that match in the [MultiQueueAuthenticator.getReservedSubQueues].
     *
     * @param includeEmpty *true* to include any empty queues which one had elements in them, otherwise *false* to only include keys from queues which have elements.
     * @return a [Set] of the available `Sub-Queues` that have entries in the [MultiQueue].
     */
    fun keys(includeEmpty: Boolean = true): Set<String>
    {
        val keysSet = keysInternal(includeEmpty)

        // Remove the reserved key(s)
        multiQueueAuthenticator.getReservedSubQueues().forEach { reservedSubQueue -> keysSet.remove(reservedSubQueue) }
        return keysSet
    }

    /**
     * Returns the `sub-queue` that the [QueueMessage] with the provided [UUID] exists in.
     *
     * @param uuid the [UUID] (as a [String]) to look up
     * @return the `sub-queue` [String] if a [QueueMessage] exists with the provided [UUID] otherwise [Optional.empty]
     */
    abstract fun containsUUID(uuid: String): Optional<String>

    /**
     * Any overridden methods to update the signature for all implementing [MultiQueue] classes.
     */
    /**
     * Override [add] method to declare [Throws] [DuplicateMessageException] annotation.
     *
     * [Synchronized] so that the call to [MultiQueue.getNextSubQueueIndex] does not cause issues, since retrieving the next index does
     * not force it to be auto incremented or unusable by another thread.
     *
     * @throws [DuplicateMessageException] if a message already exists with the same [QueueMessage.uuid] in `any` other queue.
     * @throws [IllegalSubQueueIdentifierException] if the [QueueMessage.subQueue] is invalid or reserved
     */
    @Throws(DuplicateMessageException::class, IllegalSubQueueIdentifierException::class)
    @Synchronized
    override fun add(element: QueueMessage): Boolean
    {
        if (multiQueueAuthenticator.getReservedSubQueues().contains(element.subQueue))
        {
            throw IllegalSubQueueIdentifierException(element.subQueue)
        }

        val subQueueMessageAlreadyExistsIn = containsUUID(element.uuid)
        if ( !subQueueMessageAlreadyExistsIn.isPresent)
        {
            if (element.id == null)
            {
                val index = getNextSubQueueIndex(element.subQueue)
                if (index.isPresent)
                {
                    element.id = index.get()
                }
            }
            val wasAdded = addInternal(element)
            return if (wasAdded)
            {
                LOG.debug("Added new message with uuid [{}] to sub-queue [{}].", element.uuid, element.subQueue)
                true
            }
            else
            {
                LOG.error("Failed to add message with uuid [{}] to sub-queue [{}].", element.uuid, element.subQueue)
                false
            }
        }
        else
        {
            val existingSubQueue = subQueueMessageAlreadyExistsIn.get()
            LOG.warn("Did not add new message with uuid [{}] to sub-queue [{}] as it already exists in sub-queue [{}].", element.uuid, element.subQueue, existingSubQueue)
            throw DuplicateMessageException(element.uuid, existingSubQueue)
        }
    }

    /**
     * The internal add method to be called.
     * This is not to  be called directly.
     *
     * @param element the element to add
     * @return `true` if the element was added successfully, otherwise `false`.
     */
    abstract fun addInternal(element: QueueMessage): Boolean

    @Throws(MessageDeleteException::class)
    override fun remove(element: QueueMessage): Boolean
    {
        try
        {
            val wasRemoved = removeInternal(element)
            if (wasRemoved)
            {
                LOG.debug("Removed element with UUID [{}] from sub-queue [{}].", element.uuid, element.subQueue)
            }
            else
            {
                LOG.error("Failed to remove element with UUID [{}] from sub-queue [{}].", element.uuid, element.subQueue)
            }
            return wasRemoved
        }
        catch (ex: Exception)
        {
            throw MessageDeleteException(element.uuid, ex)
        }
    }

    /**
     * The internal remove method to be called.
     * This is not to be called directly.
     *
     * @param element the element to remove
     * @return `true` if the element was removed successfully, otherwise `false`.
     */
    abstract fun removeInternal(element: QueueMessage): Boolean

    override fun contains(element: QueueMessage?): Boolean
    {
        if (element == null)
        {
            return false
        }
        val queue: Queue<QueueMessage> = getSubQueue(element.subQueue)
        return queue.contains(element)
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
            val queueForKey: Queue<QueueMessage> = getSubQueue(key)
            for(entry: QueueMessage in queueForKey)
            {
                if ( !elements.contains(entry))
                {
                    LOG.debug("Message with uuid [{}] does not exist in retain list, attempting to remove.", entry.uuid)
                    val wasRemoved = remove(entry)
                    anyWasRemoved = wasRemoved || anyWasRemoved
                    if (wasRemoved)
                    {
                        LOG.debug("Removed message with uuid [{}] as it does not exist in retain list.", entry.uuid)
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
     * @return `true` any of the [keys] returns `false` for [isEmptySubQueue], otherwise `false`.
     */
    override fun isEmpty(): Boolean
    {
        val anyHasElements = keys(false).stream().anyMatch { key -> !isEmptySubQueue(key) }
        return !anyHasElements
    }

    override fun clear()
    {
        val keys = keys()
        var removedEntryCount = 0
        for (key in keys)
        {
            val amountRemovedForQueue = clearSubQueue(key)
            removedEntryCount += amountRemovedForQueue
        }
        LOG.debug("Cleared multi-queue, removed [{}] message entries over [{}] sub-queues.", removedEntryCount, keys)
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
