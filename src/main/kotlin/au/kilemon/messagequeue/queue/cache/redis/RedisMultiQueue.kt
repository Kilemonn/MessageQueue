package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 *
 */
class RedisMultiQueue : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    override var size: Int = 0

    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, Set<QueueMessage>>

    /**
     * Append the [MessageQueueSettings.redisPrefix] to the provided [queueType] [String].
     *
     * @param queueType the [String] to add the prefix to
     * @return a [String] with the provided [queueType] type with the [MessageQueueSettings.redisPrefix] appended to the beginning.
     */
    private fun appendPrefix(queueType: String): String
    {
        return "${messageQueueSettings.redisPrefix}$queueType"
    }

    /**
     * A method to retrieve a queue type and append the prefix before requesting the underlying redis entry.
     *
     * @see [MultiQueue.getQueueForType]
     */
    private fun getQueueForTypeAppendPrefix(queueType: String): Queue<QueueMessage>
    {
        return getQueueForType(appendPrefix(queueType))
    }

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        val queue = ConcurrentLinkedQueue<QueueMessage>()
        val set = redisTemplate.opsForSet().members(queueType)
        if (!set.isNullOrEmpty())
        {
            // TODO
//            queue.addAll(set)
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

    override fun clearForType(queueType: String)
    {
        val queueForType = getQueueForType(queueType)
        if (queueForType.isNotEmpty())
        {
            val removedEntryCount = queueForType.size
            size -= removedEntryCount
            redisTemplate.opsForSet().remove(queueType)
            LOG.debug("Cleared existing queue for type [{}]. Removed [{}] message entries.", queueType, removedEntryCount)
        }
        else
        {
            LOG.debug("Attempting to clear non-existent queue for type [{}]. No messages cleared.", queueType)
        }
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        return getQueueForType(queueType).isEmpty()
    }

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        val keys = redisTemplate.keys(appendPrefix("*"))
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
                val queueForType = redisTemplate.opsForSet().members(key)
                if (!queueForType.isNullOrEmpty())
                {
                    LOG.trace("Queue type [{}] is not empty and will be returned in keys() call.", queueForType)
                    retainedKeys.add(key)
                }
            }
            LOG.debug("Removing all empty queue keys in call to keys(). Total queue keys [{}], non-empty queue keys [{}].", keys.size, retainedKeys.size)
            return retainedKeys
        }
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        // TODO
        val queueTypeForUUID: String? = null
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

    override fun remove(element: QueueMessage?): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun add(element: QueueMessage): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun clear()
    {
        super.clear()
        val keys = keys()
        val removedEntryCount = keys.size
        for (key in keys)
        {
            redisTemplate.delete(key)
        }
        LOG.debug("Cleared multi-queue, removed [{}] message entries.", removedEntryCount)
    }
}
