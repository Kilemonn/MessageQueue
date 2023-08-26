package au.kilemon.messagequeue.queue.nosql.mongo

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.message.QueueMessageDocument
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.nosql.mongo.repository.MongoQueueMessageRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class MongoMultiQueue : MultiQueue, HasLogger
{
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
        queueMessageRepository.save(queueMessageDocument)
    }

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun performHealthCheckInternal()
    {
        queueMessageRepository.existsById(1)
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        return queueMessageRepository.findByUuid(uuid)
    }

    override fun clearForTypeInternal(queueType: String): Int
    {
        TODO("Not yet implemented")
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun pollInternal(queueType: String): Optional<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        TODO("Not yet implemented")
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
}
