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
import java.util.concurrent.atomic.AtomicLong

class MongoMultiQueue : MultiQueue, HasLogger
{
    companion object
    {
        const val INDEX_ID = "index_id"
    }

    override val LOG: Logger = initialiseLogger()

    override lateinit var maxQueueIndex: HashMap<String, AtomicLong>

    @Lazy
    @Autowired
    private lateinit var queueMessageRepository: MongoQueueMessageRepository

    /**
     * Just initialise map, so it's not null, but the SQL [QueueMessage] ID is maintained by the database.
     */
    override fun initialiseQueueIndex()
    {
        maxQueueIndex = HashMap()
    }

    override fun persistMessage(message: QueueMessage)
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

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        val entries = queueMessageRepository.findByTypeOrderByIdAsc(queueType)
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

    override fun clearForTypeInternal(queueType: String): Int
    {
        val amountCleared = queueMessageRepository.deleteByType(queueType)
        LOG.debug("Cleared existing queue for type [{}]. Removed [{}] message entries.", queueType, amountCleared)
        return amountCleared
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        return queueMessageRepository.findByTypeOrderByIdAsc(queueType).isEmpty()
    }

    override fun pollInternal(queueType: String): Optional<QueueMessage>
    {
        val messages = queueMessageRepository.findByTypeOrderByIdAsc(queueType)
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
    override fun keys(includeEmpty: Boolean): Set<String>
    {
        val keySet = queueMessageRepository.getDistinctTypes().toSet()
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
    override fun getAndIncrementQueueIndex(queueType: String): Optional<Long>
    {
        return super.getAndIncrementQueueIndex(INDEX_ID)
    }

    /**
     * Override to never clear the queue index for the type, since it's a shared index map.
     */
    override fun clearQueueIndexForType(queueType: String)
    {

    }

    /**
     * Clear the [maxQueueIndex] if the entire map is cleared.
     *
     *
     * Since [clearQueueIndexForType] is not clearing any of map entries.
     */
    override fun clear()
    {
        super.clear()
        maxQueueIndex.clear()
    }
}
