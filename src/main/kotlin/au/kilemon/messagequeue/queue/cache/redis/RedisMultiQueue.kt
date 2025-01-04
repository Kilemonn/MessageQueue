package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.cache.CacheMultiQueue
import au.kilemon.messagequeue.queue.exception.IllegalSubQueueIdentifierException
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.Optional
import java.util.Queue

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Collectors
import kotlin.collections.HashSet
import kotlin.jvm.Throws

/**
 * A `Redis` specific implementation of the [MultiQueue].
 * All messages stored and accessed directly from the `Redis` cache.
 * This increasing overhead when checking UUID, but it is required incase the cache is edited manually, or by another message managing instance.
 *
 * @author github.com/Kilemonn
 */
class RedisMultiQueue(private val prefix: String) : MultiQueue(), HasLogger, CacheMultiQueue
{
    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, QueueMessage>

    @Autowired
    private lateinit var cacheKeyManager: RedisCacheKeyManager

    /**
     * @return [prefix]
     */
    override fun getPrefix(): String
    {
        return prefix
    }

    override fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>
    {
        val queue = ConcurrentLinkedQueue<QueueMessage>()
        val set = redisTemplate.opsForSet().members(appendPrefix(subQueue))
        if (!set.isNullOrEmpty())
        {
            queue.addAll(set.sortedBy { it.uuid })
        }
        return queue
    }

    override fun getAssignedMessagesInSubQueue(subQueue: String, assignedTo: String?): Queue<QueueMessage>
    {
        val queue = ConcurrentLinkedQueue<QueueMessage>()
        val existingQueue = getSubQueue(subQueue)
        if (existingQueue.isNotEmpty())
        {
            if (assignedTo == null)
            {
                queue.addAll(existingQueue.stream().filter { message -> message.assignedTo != null }.collect(Collectors.toList()))
            }
            else
            {
                queue.addAll(existingQueue.stream().filter { message -> message.assignedTo == assignedTo }.collect(Collectors.toList()))
            }
        }
        return queue
    }

    override fun performHealthCheckInternal()
    {
        redisTemplate.opsForSet().members("")
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

    @Throws(IllegalSubQueueIdentifierException::class)
    override fun addInternal(element: QueueMessage): Boolean
    {
        if (cacheKeyManager.getReservedKeys().contains(element.subQueue)
            || cacheKeyManager.getReservedKeys().contains(appendPrefix(element.subQueue)))
        {
            throw IllegalSubQueueIdentifierException(element.subQueue)
        }

        cacheKeyManager.add(appendPrefix(element.subQueue))
        val result = redisTemplate.opsForSet().add(appendPrefix(element.subQueue), element)
        return result != null && result > 0
    }

    override fun removeInternal(element: QueueMessage): Boolean
    {
        val result = redisTemplate.opsForSet().remove(appendPrefix(element.subQueue), element)
        return result != null && result > 0
    }

    override fun clearSubQueueInternal(subQueue: String): Int
    {
        var amountRemoved = 0
        val queue = getSubQueue(subQueue)
        if (queue.isNotEmpty())
        {
            amountRemoved = queue.size
            redisTemplate.delete(appendPrefix(subQueue))
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
                val sizeOfQueue = redisTemplate.opsForSet().size(key)
                if (sizeOfQueue != null && sizeOfQueue > 0)
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

    /**
     * [RedisTemplate] does not allow for inplace object updates, so we will need to remove the [message] then re-add the [message] to perform the update.
     * Since we cannot "remove" the message directly, we need to find the matching message via UUID.
     */
    override fun persistMessageInternal(message: QueueMessage)
    {
        val matchingMessage = getMessageByUUID(message.uuid)
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
}
