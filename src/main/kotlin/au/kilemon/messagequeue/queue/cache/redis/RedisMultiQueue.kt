package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.*

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
    lateinit var redisTemplate: RedisTemplate<String, QueueMessage>

    private fun appendPrefix(queueType: String): String
    {
        return "${messageQueueSettings.redisPrefix}$queueType"
    }

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>)
    {
        TODO("Not yet implemented")
    }

    override fun clearForType(queueType: String)
    {
        TODO("Not yet implemented")
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun pollForType(queueType: String): Optional<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun peekForType(queueType: String): Optional<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        return redisTemplate.keys("")
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        TODO("Not yet implemented")
    }

    override fun add(element: QueueMessage): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun remove(element: QueueMessage?): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<QueueMessage>): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun clear()
    {
        for (key in keys())
        {
            redisTemplate.delete(key)
        }
        size = 0
    }

    override fun removeAll(elements: Collection<QueueMessage>): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<QueueMessage>): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun contains(element: QueueMessage?): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<QueueMessage>): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean
    {
        return keys().isEmpty()
    }
}
