package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.exception.DuplicateMessageException
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.jvm.Throws

/**
 * The InMemoryMultiQueue which implements the [MultiQueue]. It holds a [ConcurrentHashMap] with [Queue] entries.
 * Using the provided [String], specific entries in the queue can be manipulated and changed as needed.
 *
 * @author github.com/KyleGonzalez
 */
open class InMemoryMultiQueue : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    /**
     * An internal [Map] that holds known [UUID]s (as a [String]) and their related `queueType` to quickly find entries within the [MultiQueue].
     */
    private val uuidMap: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    /**
     * The underlying [Map] holding [Queue] entities mapped against the provided [String].
     */
    private val messageQueue: ConcurrentHashMap<String, Queue<QueueMessage>> = ConcurrentHashMap()

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

    override fun clear()
    {
        super.clear()
        val removedEntryCount = messageQueue.size
        uuidMap.clear()
        messageQueue.clear()
        LOG.debug("Cleared multi-queue, removed [{}] message entries.", removedEntryCount)
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

    @Throws(DuplicateMessageException::class)
    override fun add(element: QueueMessage): Boolean
    {
        val elementIsMappedToType = containsUUID(element.uuid.toString())
        if ( !elementIsMappedToType.isPresent)
        {
            val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
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
            throw DuplicateMessageException(element.uuid.toString(), existingQueueType)
        }
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

    override fun isEmptyForType(queueType: String): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        return queueForType.isEmpty()
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

    /**
     * Update the [uuidMap] and remove the entry if it is returned (removed).
     */
    override fun pollForType(queueType: String): Optional<QueueMessage>
    {
        val message = super.pollForType(queueType)
        if (message.isPresent)
        {
            uuidMap.remove(message.get().uuid.toString())
        }

        return message
    }
}
