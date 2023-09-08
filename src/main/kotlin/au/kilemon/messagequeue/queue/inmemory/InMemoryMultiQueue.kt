package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.queue.exception.DuplicateMessageException
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors
import kotlin.collections.HashMap
import kotlin.jvm.Throws

/**
 * The InMemoryMultiQueue which implements the [MultiQueue]. It holds a [ConcurrentHashMap] with [Queue] entries.
 * Using the provided [String], specific entries in the queue can be manipulated and changed as needed.
 *
 * @author github.com/Kilemonn
 */
open class InMemoryMultiQueue : MultiQueue(), HasLogger
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

    private val maxQueueIndex: HashMap<String, AtomicLong> = HashMap()

    /**
     * This index is special compared to the other types it will be incremented once retrieved. So we could be skipping
     * indexes, but it should be fine since it's only used for message ordering.
     */
    override fun getNextQueueIndex(queueType: String): Optional<Long>
    {
        var index = maxQueueIndex[queueType]
        if (index == null)
        {
            index = AtomicLong(1)
            maxQueueIndex[queueType] = index
        }
        return Optional.of(index.getAndIncrement())
    }

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        var queueForType: Queue<QueueMessage>? = messageQueue[queueType]
        if (queueForType == null)
        {
            queueForType = ConcurrentLinkedQueue()
            LOG.debug("Initialising new queue for type [{}].", queueType)
            messageQueue[queueType] = queueForType
        }
        else
        {
            LOG.debug("Found existing queue for type [{}] with size [{}].", queueType, queueForType.size)
        }
        return queueForType
    }

    override fun performHealthCheckInternal()
    {
        // Nothing to check for the in-memory storage since there is no external storage that needs to be checked
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        val queueType = containsUUID(uuid)
        if (queueType.isPresent)
        {
            val queueForType: Queue<QueueMessage> = getQueueForType(queueType.get())
            return queueForType.stream().filter { message -> message.uuid == uuid }.findFirst()
        }
        return Optional.empty()
    }

    override fun clearForTypeInternal(queueType: String): Int
    {
        var amountRemoved = 0
        val queueForType: Queue<QueueMessage>? = messageQueue[queueType]
        maxQueueIndex.remove(queueType)
        if (queueForType != null)
        {
            amountRemoved = queueForType.size
            queueForType.forEach { message -> uuidMap.remove(message.uuid) }
            queueForType.clear()
            messageQueue.remove(queueType)
            LOG.debug("Cleared existing queue for type [{}]. Removed [{}] message entries.", queueType, amountRemoved)
        }
        else
        {
            LOG.debug("Attempting to clear non-existent queue for type [{}]. No messages cleared.", queueType)
        }
        return amountRemoved
    }

    override fun clear()
    {
        super.clear()
        maxQueueIndex.clear()
    }

    @Throws(DuplicateMessageException::class)
    override fun add(element: QueueMessage): Boolean
    {
        val wasAdded = super.add(element)
        if (wasAdded)
        {
            uuidMap[element.uuid] = element.type
        }
        return wasAdded
    }

    /**
     * Delegate to the [Queue.add] method.
     */
    override fun addInternal(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        return queueForType.add(element)
    }

    override fun remove(element: QueueMessage): Boolean
    {
        val wasRemoved  = super.remove(element)
        if (wasRemoved)
        {
            uuidMap.remove(element.uuid)
        }
        return wasRemoved
    }

    /**
     * Delegate to the [Queue.remove] method.
     */
    override fun removeInternal(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        return queueForType.remove(element)
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
            uuidMap.remove(message.get().uuid)
        }

        return message
    }

    override fun pollInternal(queueType: String): Optional<QueueMessage>
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        return if (queueForType.isNotEmpty())
        {
            Optional.of(queueForType.iterator().next())
        }
        else
        {
            Optional.empty()
        }
    }

    /**
     * Not required, since in-memory object changes are performed immediately and require not further code to persist the change
     */
    override fun persistMessageInternal(message: QueueMessage)
    {

    }
}
