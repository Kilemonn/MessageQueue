package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.sql.repository.QueueMessageRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 *
 * @author github.com/KyleGonzalez
 */
class SqlMultiQueue : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    @Lazy
    @Autowired
    private lateinit var queueMessageRepository: QueueMessageRepository

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        return ConcurrentLinkedQueue(queueMessageRepository.findByTypeOrderByIdAsc(queueType))
    }

    override fun clearForType(queueType: String): Int
    {
        return queueMessageRepository.deleteByType(queueType)
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        return queueMessageRepository.findByTypeOrderByIdAsc(queueType).isEmpty()
    }

    override fun performPoll(queueType: String): Optional<QueueMessage>
    {
        val messages = queueMessageRepository.findByTypeOrderByIdAsc(queueType)
        return if (messages.isNotEmpty())
        {
            return Optional.of(messages[0])
        }
        else
        {
            Optional.empty()
        }
    }

    /**
     * The [includeEmpty] has no benefit here, it is always `false`.
     */
    override fun keys(includeEmpty: Boolean): Set<String>
    {
        return queueMessageRepository.findDistinctType().toSet()
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        val message = queueMessageRepository.findByUuid(uuid)
        return if (message.isPresent)
        {
            Optional.of(message.get().type)
        }
        else
        {
            Optional.empty()
        }
    }

    override fun performAdd(element: QueueMessage): Boolean
    {
        val saved = queueMessageRepository.save(element)
        return saved.id != null
    }

    override fun performRemove(element: QueueMessage): Boolean
    {
        return try
        {
            queueMessageRepository.delete(element)
            true
        }
        catch (ex: Exception)
        {
            false
        }
    }
}
