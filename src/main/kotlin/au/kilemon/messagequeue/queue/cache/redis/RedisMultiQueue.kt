package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import au.kilemon.messagequeue.settings.MessageQueueSettings
import lombok.Generated
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

/**
 * A `Redis` specific implementation of the [MultiQueue].
 * All messages stored and accessed directly from the `Redis` cache.
 * This increasing overhead when checking UUID, but it is required incase the cache is edited manually, or by another message managing instance.
 *
 * @author github.com/KyleGonzalez
 */
class RedisMultiQueue(private val prefix: String = "", private val redisTemplate: RedisTemplate<String, QueueMessage>) : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    override lateinit var maxQueueIndex: HashMap<String, AtomicLong>

    /**
     * Append the [MessageQueueSettings.redisPrefix] to the provided [queueType] [String].
     *
     * @param queueType the [String] to add the prefix to
     * @return a [String] with the provided [queueType] type with the [MessageQueueSettings.redisPrefix] appended to the beginning.
     */
    private fun appendPrefix(queueType: String): String
    {
        if (prefix.isNotBlank() && !queueType.startsWith(prefix))
        {
            return "${prefix}$queueType"
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
            queue.addAll(set.toSortedSet { message1, message2 -> (message1.id ?: 0).minus(message2.id ?: 0).toInt() })
        }
        return queue
    }

    override fun getAssignedMessagesForType(queueType: String, assignedTo: String?): Queue<QueueMessage>
    {
        val queue = ConcurrentLinkedQueue<QueueMessage>()
        val set = redisTemplate.opsForSet().members(appendPrefix(queueType))
        if (!set.isNullOrEmpty())
        {
            if (assignedTo == null)
            {
                queue.addAll(set.stream().filter { message -> message.assignedTo != null }.collect(Collectors.toList()))
            }
            else
            {
                queue.addAll(set.stream().filter { message -> message.assignedTo == assignedTo }.collect(Collectors.toList()))
            }
        }
        return queue
    }

    override fun getUnassignedMessagesForType(queueType: String): Queue<QueueMessage>
    {
        val queue = ConcurrentLinkedQueue<QueueMessage>()
        val set = redisTemplate.opsForSet().members(appendPrefix(queueType))
        if (!set.isNullOrEmpty())
        {
            queue.addAll(set.stream().filter { message -> message.assignedTo == null }.collect(Collectors.toList()))
        }
        return queue
    }

    override fun performHealthCheckInternal()
    {
        redisTemplate.opsForSet().members("")
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

    override fun addInternal(element: QueueMessage): Boolean
    {
        val result = redisTemplate.opsForSet().add(appendPrefix(element.type), element)
        return result != null && result > 0
    }

    /**
     * Overriding to pass in the [queueType] into [appendPrefix].
     */
    override fun getAndIncrementQueueIndex(queueType: String): Optional<Long>
    {
        return super.getAndIncrementQueueIndex(appendPrefix(queueType))
    }

    override fun removeInternal(element: QueueMessage): Boolean
    {
        val result = redisTemplate.opsForSet().remove(appendPrefix(element.type), element)
        return result != null && result > 0
    }

    override fun clearForTypeInternal(queueType: String): Int
    {
        var amountRemoved = 0
        val queueForType = getQueueForType(queueType)
        if (queueForType.isNotEmpty())
        {
            amountRemoved = queueForType.size
            redisTemplate.delete(appendPrefix(queueType))
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

    override fun pollInternal(queueType: String): Optional<QueueMessage>
    {
        val set = redisTemplate.opsForSet().members(appendPrefix(queueType))
        if (!set.isNullOrEmpty())
        {
            return Optional.of(set.iterator().next())
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
            val anyMatchTheUUID = queue.stream().anyMatch{ message -> uuid == message.uuid }
            if (anyMatchTheUUID)
            {
                LOG.debug("Found queue type [{}] for UUID: [{}].", key, uuid)
                return Optional.of(key)
            }
        }
        LOG.debug("No queue type exists for UUID: [{}].", uuid)
        return Optional.empty()
    }

    /**
     * [RedisTemplate] does not allow for inplace object updates, so we will need to remove the [message] then re-add the [message] to perform the update.
     * Since we cannot "remove" the message directly, we need to find the matching message via UUID.
     */
    override fun persistMessage(message: QueueMessage)
    {
        val queue = getQueueForType(message.type)
        val matchingMessage = queue.stream().filter{ element -> element.uuid == message.uuid }.findFirst()
        if (matchingMessage.isPresent)
        {
            message.id = matchingMessage.get().id
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
