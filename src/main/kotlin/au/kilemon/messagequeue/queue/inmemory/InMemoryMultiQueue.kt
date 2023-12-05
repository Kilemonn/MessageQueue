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
    override val LOG: Logger = this.initialiseLogger()

    /**
     * An internal [Map] that holds known [UUID]s (as a [String]) and their related `sub-queue` to quickly find entries within the [MultiQueue].
     */
    private val uuidMap: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    /**
     * The underlying [Map] holding [Queue] entities mapped against the provided [String].
     */
    private val messageQueue: ConcurrentHashMap<String, Queue<QueueMessage>> = ConcurrentHashMap()

    private val maxQueueIndex: HashMap<String, AtomicLong> = HashMap()

    /**
     * This index is special compared to the other [au.kilemon.messagequeue.settings.StorageMedium] it will be incremented once retrieved.
     * So we could be skipping indexes, but it should be fine since it's only used for message ordering.
     */
    override fun getNextSubQueueIndex(subQueue: String): Optional<Long>
    {
        var index = maxQueueIndex[subQueue]
        if (index == null)
        {
            index = AtomicLong(1)
            maxQueueIndex[subQueue] = index
        }
        return Optional.of(index.getAndIncrement())
    }

    override fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>
    {
        var queue: Queue<QueueMessage>? = messageQueue[subQueue]
        if (queue == null)
        {
            queue = ConcurrentLinkedQueue()
            LOG.debug("Initialising new sub-queue [{}].", subQueue)
            messageQueue[subQueue] = queue
        }
        else
        {
            LOG.debug("Found existing sub-queue [{}] with size [{}].", subQueue, queue.size)
        }
        return queue
    }

    override fun performHealthCheckInternal()
    {
        // Nothing to check for the in-memory storage since there is no external storage that needs to be checked
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        val subQueue = containsUUID(uuid)
        if (subQueue.isPresent)
        {
            val queue: Queue<QueueMessage> = getSubQueue(subQueue.get())
            return queue.stream().filter { message -> message.uuid == uuid }.findFirst()
        }
        return Optional.empty()
    }

    override fun clearSubQueueInternal(subQueue: String): Int
    {
        var amountRemoved = 0
        val queue: Queue<QueueMessage>? = messageQueue[subQueue]
        maxQueueIndex.remove(subQueue)
        if (queue != null)
        {
            amountRemoved = queue.size
            queue.forEach { message -> uuidMap.remove(message.uuid) }
            queue.clear()
            messageQueue.remove(subQueue)
            LOG.debug("Cleared existing sub-queue [{}]. Removed [{}] message entries.", subQueue, amountRemoved)
        }
        else
        {
            LOG.debug("Attempting to clear non-existent sub-queue [{}]. No messages cleared.", subQueue)
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
            uuidMap[element.uuid] = element.subQueue
        }
        return wasAdded
    }

    /**
     * Delegate to the [Queue.add] method.
     */
    override fun addInternal(element: QueueMessage): Boolean
    {
        val queue: Queue<QueueMessage> = getSubQueue(element.subQueue)
        return queue.add(element)
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
        val queue: Queue<QueueMessage> = getSubQueue(element.subQueue)
        return queue.remove(element)
    }

    override fun isEmptySubQueue(subQueue: String): Boolean
    {
        val queue: Queue<QueueMessage> = getSubQueue(subQueue)
        return queue.isEmpty()
    }

    override fun keysInternal(includeEmpty: Boolean): HashSet<String>
    {
        if (includeEmpty)
        {
            LOG.debug("Including all empty queue keys in call to keys(). Total queue keys [{}].", messageQueue.keys.size)
            return HashSet(messageQueue.keys)
        }
        else
        {
            val keys = HashSet<String>()
            for (key: String in messageQueue.keys)
            {
                val queue = getSubQueue(key)
                if (queue.isNotEmpty())
                {
                    LOG.trace("Sub-queue [{}] is not empty and will be returned in keys() call.", queue)
                    keys.add(key)
                }
            }
            LOG.debug("Removing all empty queue keys in call to keys(). Total queue keys [{}], non-empty queue keys [{}].", messageQueue.keys.size, keys.size)
            return keys
        }
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        val subQueueID: String? = uuidMap[uuid]
        if (subQueueID.isNullOrBlank())
        {
            LOG.debug("No sub-queue exists for UUID: [{}].", uuid)
        }
        else
        {
            LOG.debug("Found sub-queue [{}] for UUID: [{}].", subQueueID, uuid)
        }
        return Optional.ofNullable(subQueueID)
    }

    /**
     * Update the [uuidMap] and remove the entry if it is returned (removed).
     */
    override fun pollSubQueue(subQueue: String): Optional<QueueMessage>
    {
        val message = super.pollSubQueue(subQueue)
        if (message.isPresent)
        {
            uuidMap.remove(message.get().uuid)
        }

        return message
    }

    override fun pollInternal(subQueue: String): Optional<QueueMessage>
    {
        val queue: Queue<QueueMessage> = getSubQueue(subQueue)
        return if (queue.isNotEmpty())
        {
            Optional.of(queue.iterator().next())
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
