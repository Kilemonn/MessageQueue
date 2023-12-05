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
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A NoSql mongo backed [MultiQueue]. All operations are performed directly on the database it is the complete source of truth.
 * It allows the messages to never go out of sync in a case where there are multiple [MultiQueue]s working on the same data source.
 *
 * @author github.com/Kilemonn
 */
class MongoMultiQueue : MultiQueue(), HasLogger
{
    companion object
    {
        const val INDEX_ID = "index_id"
    }

    override val LOG: Logger = this.initialiseLogger()

    @Lazy
    @Autowired
    private lateinit var queueMessageRepository: MongoQueueMessageRepository

    override fun persistMessageInternal(message: QueueMessage)
    {
        val queueMessageDocument = QueueMessageDocument(message)
        try
        {
            queueMessageRepository.save(queueMessageDocument)
        }
        catch (ex: Exception)
        {
            throw MessageUpdateException(message.uuid, ex)
        }
    }

    /**
     * Overriding to use more direct optimised queries.
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

        return ConcurrentLinkedQueue(entries.map { entry -> QueueMessage(entry) })
    }

    /**
     * Overriding since we can filter via the DB query.
     */
    override fun getUnassignedMessagesInSubQueue(subQueue: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findBySubQueueAndAssignedToIsNullOrderByIdAsc(subQueue)
        return ConcurrentLinkedQueue(entries.map { entry -> QueueMessage(entry) })
    }

    override fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findBySubQueueOrderByIdAsc(subQueue)
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
            Optional.of(QueueMessage(documentMessage.get()))
        }
        else
        {
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
        return queueMessageRepository.findBySubQueueOrderByIdAsc(subQueue).isEmpty()
    }

    override fun pollInternal(subQueue: String): Optional<QueueMessage>
    {
        val messages = queueMessageRepository.findBySubQueueOrderByIdAsc(subQueue)
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
        val saved = queueMessageRepository.save(queueMessageDocument)
        return saved.id != null
    }

    override fun removeInternal(element: QueueMessage): Boolean
    {
        val removedCount = queueMessageRepository.deleteByUuid(element.uuid)
        return removedCount > 0
    }

    /**
     * Overriding to use the constant [INDEX_ID] for all look-ups since the ID is shared and needs to be assigned to
     * the [QueueMessageDocument] before it is created.
     */
    override fun getNextSubQueueIndex(subQueue: String): Optional<Long>
    {
        val largestIdMessage = queueMessageRepository.findTopByOrderByIdDesc()
        return if (largestIdMessage.isPresent)
        {
            Optional.of(largestIdMessage.get().id?.plus(1) ?: 1)
        }
        else
        {
            Optional.of(1)
        }
    }
}
