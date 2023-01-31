package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.exception.HealthCheckFailureException
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import au.kilemon.messagequeue.queue.sql.repository.QueueMessageRepository
import au.kilemon.messagequeue.settings.MessageQueueSettings
import lombok.Generated
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

    @Autowired
    @Lazy
    @get:Generated
    @set:Generated
    lateinit var messageQueueSettings: MessageQueueSettings

    @Lazy
    @Autowired
    private lateinit var queueMessageRepository: QueueMessageRepository

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findByTypeOrderByIdAsc(queueType)
        return ConcurrentLinkedQueue(entries.map { entry -> entry.resolvePayloadObject() })
    }

    override fun getAssignedMessagesForType(queueType: String, assignedTo: String?): Queue<QueueMessage>
    {
        val entries = if (assignedTo == null)
        {
            queueMessageRepository.findByTypeAndAssignedToIsNotNullOrderByIdAsc(queueType)
        }
        else
        {
            queueMessageRepository.findByTypeAndAssignedToOrderByIdAsc(queueType, assignedTo)
        }

        return ConcurrentLinkedQueue(entries.map { entry -> entry.resolvePayloadObject() })
    }

    override fun getUnassignedMessagesForType(queueType: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findByTypeAndAssignedToIsNullOrderByIdAsc(queueType)
        return ConcurrentLinkedQueue(entries.map { entry -> entry.resolvePayloadObject() })
    }

    override fun performHealthCheckInternal()
    {
        queueMessageRepository.existsById(1)
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        return queueMessageRepository.findByUuid(uuid)
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
        val removedCount = queueMessageRepository.deleteByUuid(element.uuid)
        return removedCount > 0
    }

    override fun persistMessage(message: QueueMessage)
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
}
