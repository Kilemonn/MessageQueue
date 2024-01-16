package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import au.kilemon.messagequeue.queue.sql.repository.SqlQueueMessageRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A database backed [MultiQueue]. All operations are performed directly on the database it is the complete source of truth.
 * It allows the messages to never go out of sync in a case where there are multiple [MultiQueue]s working on the same data source.
 *
 * @author github.com/Kilemonn
 */
class SqlMultiQueue : MultiQueue(), HasLogger
{
    override val LOG: Logger = this.initialiseLogger()

    @Lazy
    @Autowired
    private lateinit var queueMessageRepository: SqlQueueMessageRepository

    override fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findBySubQueueOrderByIdAsc(subQueue)
        return ConcurrentLinkedQueue(entries.map { entry -> entry.resolvePayloadObject() })
    }

    /**
     * Overriding since we can filter via the DB query.
     */
    override fun getAssignedMessagesInSubQueue(subQueue: String, assignedTo: String?): Queue<QueueMessage>
    {
        val entries = if (assignedTo == null)
        {
            queueMessageRepository.findBySubQueueAndAssignedToIsNotNullOrderByIdAsc(subQueue)
        }
        else
        {
            queueMessageRepository.findBySubQueueAndAssignedToOrderByIdAsc(subQueue, assignedTo)
        }

        return ConcurrentLinkedQueue(entries.map { entry -> entry.resolvePayloadObject() })
    }

    /**
     * Overriding since we can filter via the DB query.
     */
    override fun getUnassignedMessagesInSubQueue(subQueue: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findBySubQueueAndAssignedToIsNullOrderByIdAsc(subQueue)
        return ConcurrentLinkedQueue(entries.map { entry -> entry.resolvePayloadObject() })
    }

    override fun performHealthCheckInternal()
    {
        queueMessageRepository.existsById(1)
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        val message = queueMessageRepository.findByUuid(uuid)
        return if (message.isPresent)
        {
            LOG.trace("Found message with uuid [{}].", uuid)
            Optional.of(message.get().resolvePayloadObject())
        }
        else
        {
            LOG.trace("No message found with uuid [{}].", uuid)
            Optional.empty()
        }
    }

    override fun clearSubQueueInternal(subQueue: String): Int
    {
        val amountCleared = queueMessageRepository.deleteBySubQueue(subQueue)
        LOG.debug("Cleared existing sub-queue [{}]. Removed [{}] message entries.", subQueue, amountCleared)
        return amountCleared
    }

    override fun isEmptySubQueue(subQueue: String): Boolean
    {
        return queueMessageRepository.findBySubQueueOrderByIdAsc(subQueue).isEmpty()
    }

    override fun pollInternal(subQueue: String): Optional<QueueMessage>
    {
        val messages = queueMessageRepository.findBySubQueueOrderByIdAsc(subQueue)
        return if (messages.isNotEmpty())
        {
            return Optional.of(messages[0].resolvePayloadObject())
        }
        else
        {
            Optional.empty()
        }
    }

    /**
     * The [includeEmpty] value makes no difference it is always effectively `false`.
     */
    override fun keysInternal(includeEmpty: Boolean): HashSet<String>
    {
        val keySet = HashSet(queueMessageRepository.findDistinctSubQueue())
        LOG.debug("Total amount of queue keys [{}].", keySet.size)
        return keySet
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        val optionalMessage = queueMessageRepository.findByUuid(uuid)
        return if (optionalMessage.isPresent)
        {
            val message = optionalMessage.get()
            LOG.debug("Found sub-queue [{}] for message with UUID: [{}].", message.subQueue, uuid)
            Optional.of(message.subQueue)
        }
        else
        {
            LOG.debug("No sub-queue found for message with UUID: [{}].", uuid)
            Optional.empty()
        }
    }

    override fun addInternal(element: QueueMessage): Boolean
    {
        // UUID Unique constraint ensures we don't save duplicate entries
        // Not need to set [QueueMessage.id] since it's managed by the DB
        val saved = queueMessageRepository.save(element)
        return saved.id != null
    }

    override fun removeInternal(element: QueueMessage): Boolean
    {
        val removedCount = queueMessageRepository.deleteByUuid(element.uuid)
        return removedCount > 0
    }

    override fun persistMessageInternal(message: QueueMessage)
    {
        // We are working with an object from JPA if there is an existing ID
        // If there is no id in the provided message then we will check that the message with the same UUID does exist
        if (message.id != null || queueMessageRepository.findByUuid(message.uuid).isPresent)
        {
            val saved = queueMessageRepository.save(message)
            if (saved == message)
            {
                return
            }
        }
        throw MessageUpdateException(message.uuid)
    }

    /**
     * Overriding to return [Optional.EMPTY] so that the [MultiQueue.add] does set an `id` into the [QueueMessage]
     * even if the id is `null`.
     */
    override fun getNextSubQueueIndex(subQueue: String): Optional<Long>
    {
        return Optional.empty()
    }
}
