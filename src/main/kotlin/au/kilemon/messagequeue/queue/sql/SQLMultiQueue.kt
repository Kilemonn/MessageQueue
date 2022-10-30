package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import org.slf4j.Logger
import java.util.*

/**
 *
 */
class SQLMultiQueue : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

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
