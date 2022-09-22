package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The InMemoryMultiQueue which implements the [MultiQueue]. It holds a [ConcurrentHashMap] with [Queue] entries.
 * Using the provided [String], specific entries in the queue can be manipulated and changed as needed.
 *
 * @author github.com/KyleGonzalez
 */
open class InMemoryMultiQueue: MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    /**
     * The underlying [Map] holding [Queue] entities mapped against the provided [String].
     */
    private val messageQueue: ConcurrentHashMap<String, Queue<QueueMessage>> = ConcurrentHashMap()

    /**
     * An internal [Map] that holds known [UUID]s (as a [String]) and their related `queueType` to quickly find entries within the [MultiQueue].
     */
    private val uuidMap: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    override var size: Int = 0

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        var queueForType: Queue<QueueMessage>? = messageQueue[queueType]
        if (queueForType == null)
        {
            LOG.debug("No existing queue found for type [{}].", queueType)
            queueForType = ConcurrentLinkedQueue()
            this.initialiseQueueForType(queueType, queueForType)
        }
        else
        {
            LOG.debug("Found existing queue for type [{}] with size [{}].", queueType, queueForType.size)
        }
        return queueForType
    }

    override fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>)
    {
        LOG.debug("Initialising new queue for type [{}].", queueType)
        messageQueue[queueType] = queue
    }

    override fun add(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        val elementIsMappedToType = containsUUID(element.uuid.toString())
        if ( !elementIsMappedToType.isPresent)
        {
            val wasAdded = queueForType.add(element)
            return if (wasAdded)
            {
                uuidMap[element.uuid.toString()] = element.type
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
            return false
        }
    }

    override fun addAll(elements: Collection<QueueMessage>): Boolean
    {
        var wasAdded = false
        for (element: QueueMessage in elements)
        {
            wasAdded = add(element) || wasAdded
        }
        return wasAdded
    }

    override fun clear()
    {
        val removedEntryCount = messageQueue.size
        messageQueue.clear()
        uuidMap.clear()
        size = 0
        LOG.debug("Cleared multiqueue, removed [{}] message entries.", removedEntryCount)
    }

    override fun clearForType(queueType: String)
    {
        val queueForType: Queue<QueueMessage>? = messageQueue[queueType]
        if (queueForType != null)
        {
            val removedEntryCount = queueForType.size
            size -= removedEntryCount
            queueForType.forEach { message -> uuidMap.remove(message.uuid.toString()) }
            queueForType.clear()
            LOG.debug("Cleared existing queue for type [{}]. Removed [{}] message entries.", queueType, removedEntryCount)
        }
        else
        {
            LOG.debug("Attempting to clear non-existent queue for type [{}]. No messages cleared.", queueType)
        }
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
                        uuidMap.remove(entry.uuid.toString())
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

    override fun remove(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        val wasRemoved = queueForType.remove(element)
        if (wasRemoved)
        {
            uuidMap.remove(element.uuid.toString())
            size--
            LOG.debug("Removed element with UUID [{}] from queue with type [{}].", element.uuid, element.type)
        }
        else
        {
            LOG.error("Failed to remove element with UUID [{}] from queue with type [{}].", element.uuid, element.type)
        }
        return wasRemoved
    }

    override fun isEmpty(): Boolean
    {
        return size == 0
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        return queueForType.isEmpty()
    }

    override fun pollForType(queueType: String): Optional<QueueMessage>
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        val head = queueForType.poll()
        if (head != null)
        {
            LOG.debug("Found and removed head element with UUID [{}] from queue with type [{}].", head.uuid, queueType)
            uuidMap.remove(head.uuid.toString())
            size--
        }
        else
        {
            LOG.debug("No head element found when polling queue with type [{}].", queueType)
        }
        return Optional.ofNullable(head)
    }

    override fun peekForType(queueType: String): Optional<QueueMessage>
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

    override fun containsAll(elements: Collection<QueueMessage>): Boolean
    {
        return elements.stream().allMatch{ element -> this.contains(element) }
    }

    override fun contains(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        return queueForType.contains(element)
    }

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        if (includeEmpty)
        {
            LOG.debug("Including all empty queue keys in call to keys(). Total queue keys [{}].", messageQueue.keys.size)
            return messageQueue.keys.toSet()
        }
        else
        {
            val keys = HashSet<String>()
            for (key: String in messageQueue.keys)
            {
                val queueForType = getQueueForType(key)
                if (queueForType.isNotEmpty())
                {
                    LOG.trace("Queue type [{}] is not empty and will be returned in keys() call.", queueForType)
                    keys.add(key)
                }
            }
            LOG.debug("Removing all empty queue keys in call to keys(). Total queue keys [{}], non-empty queue keys [{}].", messageQueue.keys.size, keys.size)
            return keys
        }
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        val queueTypeForUUID: String? = uuidMap[uuid]
        if (queueTypeForUUID.isNullOrBlank())
        {
            LOG.debug("No queue type exists for UUID: [{}].", uuid)
        }
        else
        {
            LOG.debug("Found queue type [{}] for UUID: [{}].", queueTypeForUUID, uuid)
        }
        return Optional.ofNullable(queueTypeForUUID)
    }
}
