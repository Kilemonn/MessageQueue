package au.kilemon.messagequeue.queue.cache.memcached

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.cache.CacheMultiQueue
import au.kilemon.messagequeue.queue.exception.IllegalSubQueueIdentifierException
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import net.rubyeye.xmemcached.MemcachedClient
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import java.util.Optional
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashSet

/**
 * A `Memcached` specific implementation of the [MultiQueue].
 * All messages stored and accessed directly from the `Memcached` instance.
 * This increases overhead when checking UUID, but it is required incase the cache is edited manually, or by another message managing instance.
 *
 * @author github.com/Kilemonn
 */
class MemcachedMultiQueue(private val prefix: String): MultiQueue(), HasLogger, CacheMultiQueue
{
    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    private lateinit var client: MemcachedClient

    @Autowired
    private lateinit var cacheKeyManager: MemcachedCacheKeyManager

    /**
     * @return [prefix]
     */
    override fun getPrefix(): String
    {
        return prefix
    }

    override fun persistMessageInternal(message: QueueMessage)
    {
        val queue = getSubQueue(message.subQueue)
        val matchingMessage = queue.stream().filter{ element -> element.uuid == message.uuid }.findFirst()
        if (matchingMessage.isPresent)
        {
            val wasRemoved = removeInternal(matchingMessage.get())
            val wasReAdded = addInternal(message)
            if (wasRemoved && wasReAdded)
            {
                return
            }
        }
        throw MessageUpdateException(message.uuid)
    }

    override fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>
    {
        var queue: Queue<QueueMessage>? = client.get<Queue<QueueMessage>?>(appendPrefix(subQueue))
        if (queue == null)
        {
            queue = ConcurrentLinkedQueue<QueueMessage>()
            client.set(appendPrefix(subQueue), 0, queue)
        }

        // Memcached does not guarantee the order, so we need to order it ourselves
        return ConcurrentLinkedQueue(queue.sortedBy { it.uuid })
    }

    override fun performHealthCheckInternal()
    {
        client.get<Any?>("health-check-key")
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        val subQueue = containsUUID(uuid)
        if (subQueue.isPresent)
        {
            LOG.trace("Found message with uuid [{}].", uuid)
            val queue: Queue<QueueMessage> = getSubQueue(subQueue.get())
            return queue.stream().filter { message -> message.uuid == uuid }.findFirst()
        }
        LOG.trace("No message found with uuid [{}].", uuid)
        return Optional.empty()
    }

    override fun clearSubQueueInternal(subQueue: String): Int
    {
        var amountRemoved = 0
        val queue = getSubQueue(subQueue)
        if (queue.isNotEmpty())
        {
            amountRemoved = queue.size
            client.delete(appendPrefix(subQueue))
            LOG.debug("Cleared existing sub-queue [{}]. Removed [{}] message entries.", subQueue, amountRemoved)
        }
        else
        {
            LOG.debug("Attempting to clear non-existent sub-queue [{}]. No messages cleared.", subQueue)
        }
        cacheKeyManager.remove(appendPrefix(subQueue))
        return amountRemoved
    }

    override fun isEmptySubQueue(subQueue: String): Boolean
    {
        return getSubQueue(subQueue).isEmpty()
    }

    override fun pollInternal(subQueue: String): Optional<QueueMessage>
    {
        val queue = getSubQueue(subQueue)
        if (queue.isNotEmpty())
        {
            return Optional.of(queue.iterator().next())
        }
        return Optional.empty()
    }

    override fun keysInternal(includeEmpty: Boolean): HashSet<String>
    {
        val keys = cacheKeyManager.getKeys()
        if (includeEmpty)
        {
            LOG.debug("Including all empty queue keys in call to keys(). Total queue keys [{}].", keys.size)
            return keys
        }
        else
        {
            val retainedKeys = HashSet<String>()
            for (key: String in keys)
            {
                val sizeOfQueue = getSubQueue(key).size
                if (sizeOfQueue > 0)
                {
                    LOG.trace("Sub-queue [{}] is not empty and will be returned in keys() call.", key)
                    retainedKeys.add(key)
                }
            }
            LOG.debug("Removing all empty queue keys in call to keys(). Total queue keys [{}], non-empty queue keys [{}].", keys.size, retainedKeys.size)
            return retainedKeys
        }
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        for (key in keys())
        {
            val queue = getSubQueue(key)
            val anyMatchTheUUID = queue.stream().anyMatch{ message -> uuid == message.uuid }
            if (anyMatchTheUUID)
            {
                LOG.debug("Found sub-queue [{}] for message UUID: [{}].", key, uuid)
                return Optional.of(key)
            }
        }
        LOG.debug("No sub-queue contains message with UUID: [{}].", uuid)
        return Optional.empty()
    }

    override fun addInternal(element: QueueMessage): Boolean
    {
        if (cacheKeyManager.getReservedKeys().contains(element.subQueue)
            || cacheKeyManager.getReservedKeys().contains(appendPrefix(element.subQueue)))
        {
            throw IllegalSubQueueIdentifierException(element.subQueue)
        }

        val queue: Queue<QueueMessage> = getSubQueue(element.subQueue)
        val wasAdded = queue.add(element)
        cacheKeyManager.add(appendPrefix(element.subQueue))
        return wasAdded && client.set(appendPrefix(element.subQueue), 0, queue)
    }

    override fun removeInternal(element: QueueMessage): Boolean
    {
        val queue: Queue<QueueMessage> = getSubQueue(element.subQueue)
        val wasRemoved = queue.remove(element)
        return wasRemoved && client.set(appendPrefix(element.subQueue), 0, queue)
    }
}
