package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.sql.repository.QueueMessageRepository
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import java.util.*

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
        TODO("Not yet implemented")
    }

    override fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>)
    {
        TODO("Not yet implemented")
    }

    override fun clearForType(queueType: String): Int
    {
        TODO("Not yet implemented")
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun performPoll(queueType: String): Optional<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        TODO("Not yet implemented")
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        TODO("Not yet implemented")
    }

    override fun performAdd(element: QueueMessage): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun performRemove(element: QueueMessage): Boolean
    {
        TODO("Not yet implemented")
    }
}
