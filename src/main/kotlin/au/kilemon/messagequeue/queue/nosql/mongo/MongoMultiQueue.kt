package au.kilemon.messagequeue.queue.nosql.mongo

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.message.QueueMessageDocument
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import au.kilemon.messagequeue.queue.nosql.mongo.repository.MongoQueueMessageRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import java.util.Optional
import java.util.Queue

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A NoSql mongo backed [MultiQueue]. All operations are performed directly on the database it is the complete source of truth.
 * It allows the messages to never go out of sync in a case where there are multiple [MultiQueue]s working on the same data source.
 *
 * @author github.com/Kilemonn
 */
class MongoMultiQueue : MultiQueue(), HasLogger
{
    override val LOG: Logger = this.initialiseLogger()

    @Lazy
    @Autowired
    private lateinit var queueMessageRepository: MongoQueueMessageRepository

    override fun persistMessageInternal(message: QueueMessage)
    {
        if (queueMessageRepository.findByUuid(message.uuid).isPresent)
        {
            val queueMessageDocument = QueueMessageDocument(message)
            queueMessageRepository.save(queueMessageDocument)
            return
        }
        throw MessageUpdateException(message.uuid)
    }

    /**
     * Overriding to use more direct optimised queries.
     */
    override fun getAssignedMessagesInSubQueue(subQueue: String, assignedTo: String?): Queue<QueueMessage>
    {
        val entries = if (assignedTo == null)
        {
            queueMessageRepository.findBySubQueueAndAssignedToIsNotNullOrderByUuidAsc(subQueue)
        }
        else
        {
            queueMessageRepository.findBySubQueueAndAssignedToOrderByUuidAsc(subQueue, assignedTo)
        }

        return ConcurrentLinkedQueue(entries.map { entry -> QueueMessage(entry) })
    }

    /**
     * Overriding since we can filter via the DB query.
     */
    override fun getUnassignedMessagesInSubQueue(subQueue: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findBySubQueueAndAssignedToIsNullOrderByUuidAsc(subQueue)
        return ConcurrentLinkedQueue(entries.map { entry -> QueueMessage(entry) })
    }

    override fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findBySubQueueOrderByUuidAsc(subQueue)
        return ConcurrentLinkedQueue(entries.map { entry -> QueueMessage(entry) })
    }

    override fun performHealthCheckInternal()
    {
        queueMessageRepository.existsById(1)
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        val documentMessage = queueMessageRepository.findByUuid(uuid)
        return if (documentMessage.isPresent)
        {
            LOG.trace("Found message with uuid [{}].", uuid)
            Optional.of(QueueMessage(documentMessage.get()))
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
        LOG.debug("Cleared existing queue for sub-queue [{}]. Removed [{}] message entries.", subQueue, amountCleared)
        return amountCleared
    }

    override fun isEmptySubQueue(subQueue: String): Boolean
    {
        return queueMessageRepository.findBySubQueueOrderByUuidAsc(subQueue).isEmpty()
    }

    override fun pollInternal(subQueue: String): Optional<QueueMessage>
    {
        val messages = queueMessageRepository.findBySubQueueOrderByUuidAsc(subQueue)
        return if (messages.isNotEmpty())
        {
            return Optional.of(QueueMessage(messages[0]))
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
        val keySet = HashSet(queueMessageRepository.getDistinctSubQueues())
        LOG.debug("Total amount of queue keys [{}].", keySet.size)
        return keySet
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        val optionalMessage = queueMessageRepository.findByUuid(uuid)
        return if (optionalMessage.isPresent)
        {
            val message = optionalMessage.get()
            LOG.debug("Found sub-queue [{}] for UUID: [{}].", message.subQueue, uuid)
            Optional.of(message.subQueue)
        }
        else
        {
            LOG.debug("No sub-queue exists for UUID: [{}].", uuid)
            Optional.empty()
        }
    }

    override fun addInternal(element: QueueMessage): Boolean
    {
        val queueMessageDocument = QueueMessageDocument(element)
        try
        {
            queueMessageRepository.save(queueMessageDocument)
            return true
        }
        catch (ex: Exception)
        {
            LOG.error("Failed to add message to sub queue [{}]", element.subQueue, ex)
            return false
        }
    }

    override fun removeInternal(element: QueueMessage): Boolean
    {
        val removedCount = queueMessageRepository.deleteByUuid(element.uuid)
        return removedCount > 0
    }
}
