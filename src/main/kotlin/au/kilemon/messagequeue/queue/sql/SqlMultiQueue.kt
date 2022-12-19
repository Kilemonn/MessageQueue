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
 * A database backed [MultiQueue]. All operations are performed directly on the database it is the complete source of truth.
 * It allows the messages to never go out of sync in a case where there are multiple [MultiQueue]s working on the same data source.
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
        val amountCleared = queueMessageRepository.deleteByType(queueType)
        LOG.debug("Cleared existing queue for type [{}]. Removed [{}] message entries.", queueType, amountCleared)
        return amountCleared
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
     * The [includeEmpty] value makes no difference it is always effectively `false`.
     */
    override fun keys(includeEmpty: Boolean): Set<String>
    {
        val keySet = queueMessageRepository.findDistinctType().toSet()
        LOG.debug("Total amount of queue keys [{}].", keySet.size)
        return keySet
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        val optionalMessage = queueMessageRepository.findByUuid(uuid)
        return if (optionalMessage.isPresent)
        {
            val message = optionalMessage.get()
            LOG.debug("Found queue type [{}] for UUID: [{}].", message.type, uuid)
            Optional.of(message.type)
        }
        else
        {
            LOG.debug("No queue type exists for UUID: [{}].", uuid)
            Optional.empty()
        }
    }

    override fun performAdd(element: QueueMessage): Boolean
    {
        // I would check that the saved is the same as the incoming element
        // But we ensure the same message does not already exist (with the same UUID)
        val saved = queueMessageRepository.save(element)
        return saved.id != null
    }

    override fun performRemove(element: QueueMessage): Boolean
    {
        val removedCount = queueMessageRepository.deleteByUuid(element.uuid.toString())
        return removedCount > 0
    }
}
