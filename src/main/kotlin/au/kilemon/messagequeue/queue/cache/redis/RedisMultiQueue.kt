package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A `Redis` specific implementation of the [MultiQueue].
 * All messages stored and accessed directly from the `Redis` cache.
 * This increasing overhead when checking UUID, but it is required incase the cache is edited manually, or by another message managing instance.
 *
 * @author github.com/KyleGonzalez
 */
class RedisMultiQueue : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    @Autowired
    @Lazy
    lateinit var messageQueueSettings: MessageQueueSettings

    @Autowired
    @Lazy
    lateinit var redisTemplate: RedisTemplate<String, QueueMessage>

    /**
     * Append the [MessageQueueSettings.redisPrefix] to the provided [queueType] [String].
     *
     * @param queueType the [String] to add the prefix to
     * @return a [String] with the provided [queueType] type with the [MessageQueueSettings.redisPrefix] appended to the beginning.
     */
    private fun appendPrefix(queueType: String): String
    {
        if (messageQueueSettings.redisPrefix.isNotBlank() && !queueType.startsWith(messageQueueSettings.redisPrefix))
        {
            return "${messageQueueSettings.redisPrefix}$queueType"
        }
        return queueType
    }

    /**
     * Attempts to append the prefix before requesting the underlying redis entry if the provided [queueType] is not prefixed with [MessageQueueSettings.redisPrefix].
     */
    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        val queue = ConcurrentLinkedQueue<QueueMessage>()
        val set = redisTemplate.opsForSet().members(appendPrefix(queueType))
        if (!set.isNullOrEmpty())
        {
            queue.addAll(set)
        }
        return queue
    }

    /**
     * Not required for Redis as we can add messages directly without initialising the cache.
     */
    override fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>)
    {
        // Not used
    }

    override fun performAdd(element: QueueMessage): Boolean
    {
        val result = redisTemplate.opsForSet().add(appendPrefix(element.type), element)
        return result != null && result > 0
    }

    override fun performRemove(element: QueueMessage): Boolean
    {
        val result = redisTemplate.opsForSet().remove(appendPrefix(element.type), element)
        return result != null && result > 0
    }

    override fun clearForType(queueType: String): Int
    {
        var amountRemoved = 0
        val queueForType = getQueueForType(queueType)
        if (queueForType.isNotEmpty())
        {
            amountRemoved = queueForType.size
            redisTemplate.delete(queueType)
            LOG.debug("Cleared existing queue for type [{}]. Removed [{}] message entries.", queueType, amountRemoved)
        }
        else
        {
            LOG.debug("Attempting to clear non-existent queue for type [{}]. No messages cleared.", queueType)
        }
        return amountRemoved
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        return getQueueForType(queueType).isEmpty()
    }

    override fun performPoll(queueType: String): Optional<QueueMessage>
    {
        val set = redisTemplate.opsForSet().members(appendPrefix(queueType))
        if (!set.isNullOrEmpty())
        {
            val next = set.iterator().next()
            redisTemplate.opsForSet().remove(appendPrefix(queueType), next)
            return Optional.of(next)
        }
        return Optional.empty()
    }

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        val scanOptions = ScanOptions.scanOptions().match(appendPrefix("*")).build()
        val cursor = redisTemplate.scan(scanOptions)
        val keys = HashSet<String>()
        cursor.forEach { element -> keys.add(element) }
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
                    LOG.trace("Queue type [{}] is not empty and will be returned in keys() call.", key)
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
            val queue = getQueueForType(key)
            val anyMatchTheUUID = queue.stream().anyMatch{ message -> uuid == message.uuid.toString() }
            if (anyMatchTheUUID)
            {
                LOG.debug("Found queue type [{}] for UUID: [{}].", key, uuid)
                return Optional.of(key)
            }
        }
        LOG.debug("No queue type exists for UUID: [{}].", uuid)
        return Optional.empty()
    }
}
