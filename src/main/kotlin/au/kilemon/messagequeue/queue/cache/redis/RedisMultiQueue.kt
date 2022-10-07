package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import org.slf4j.Logger
import java.util.*

/**
 *
 */
class RedisMultiQueue : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    override var size: Int = 0

    override fun getQueueForType(queueType: String): Queue<QueueMessage> {
        TODO("Not yet implemented")
    }

    override fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>) {
        TODO("Not yet implemented")
    }

    override fun clearForType(queueType: String) {
        TODO("Not yet implemented")
    }

    override fun isEmptyForType(queueType: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun pollForType(queueType: String): Optional<QueueMessage> {
        TODO("Not yet implemented")
    }

    override fun peekForType(queueType: String): Optional<QueueMessage> {
        TODO("Not yet implemented")
    }

    override fun keys(includeEmpty: Boolean): Set<String> {
        TODO("Not yet implemented")
    }

    override fun containsUUID(uuid: String): Optional<String> {
        TODO("Not yet implemented")
    }

    override fun add(element: QueueMessage): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(element: QueueMessage?): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<QueueMessage>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<QueueMessage>): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<QueueMessage>): Boolean {
        TODO("Not yet implemented")
    }

    override fun contains(element: QueueMessage?): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<QueueMessage>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }
}
