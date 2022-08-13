package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.type.QueueTypeProvider
import lombok.extern.slf4j.Slf4j
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The InMemoryMultiQueue which implements the [MultiQueue]. It holds a [ConcurrentHashMap] with [Queue] entries.
 * Using the provided [QueueTypeProvider], specific entries in the queue can be manipulated and changed as needed.
 *
 * @author github.com/KyleGonzalez
 */
@Slf4j
open class InMemoryMultiQueue: MultiQueue
{
    /**
     * The underlying [Map] holding [Queue] entities mapped against the provided [String].
     */
    private val messageQueue: ConcurrentHashMap<String, Queue<QueueMessage>> = ConcurrentHashMap()

    override var size: Int = 0

    override fun getQueueForType(queueTypeProvider: QueueTypeProvider): Queue<QueueMessage>
    {
        var queueForType: Queue<QueueMessage>? = messageQueue[queueTypeProvider.getIdentifier()]
        if (queueForType == null)
        {
            queueForType = ConcurrentLinkedQueue()
            this.initialiseQueueForType(queueTypeProvider, queueForType)
        }
        return queueForType
    }

    override fun initialiseQueueForType(queueTypeProvider: QueueTypeProvider, queue: Queue<QueueMessage>)
    {
        messageQueue[queueTypeProvider.getIdentifier()] = queue
    }

    override fun add(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        val wasAdded = queueForType.add(element)
        if (wasAdded)
        {
            size++
        }
        return wasAdded
    }

    override fun addAll(elements: Collection<QueueMessage>): Boolean
    {
        var wasAdded = false
        for (element: QueueMessage in elements)
        {
            wasAdded = add(element) || wasAdded
        }
        return wasAdded
    }

    override fun clear()
    {
        messageQueue.clear()
        size = 0
    }

    override fun clearForType(queueTypeProvider: QueueTypeProvider)
    {
        val queueForType: Queue<QueueMessage>? = messageQueue[queueTypeProvider.getIdentifier()]
        if (queueForType != null)
        {
            size -= queueForType.size
            queueForType.clear()
        }
    }

    override fun retainAll(elements: Collection<QueueMessage>): Boolean
    {
        var anyWasRemoved = false
        for (key: String in messageQueue.keys())
        {
            val queueForKey: Queue<QueueMessage>? = messageQueue[key]
            if (queueForKey != null)
            {
                for(entry: QueueMessage in queueForKey)
                {
                    if (!elements.contains(entry))
                    {
                        val wasRemoved = queueForKey.remove(entry)
                        anyWasRemoved = wasRemoved || anyWasRemoved
                        if (wasRemoved)
                        {
                            size--
                        }
                    }
                }
            }
        }
        return anyWasRemoved
    }

    override fun removeAll(elements: Collection<QueueMessage>): Boolean
    {
        var wasRemoved = false
        for (element: QueueMessage in elements)
        {
            wasRemoved = remove(element) || wasRemoved
        }
        return wasRemoved
    }

    override fun remove(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        val wasRemoved = queueForType.remove(element)
        if (wasRemoved)
        {
            size--
        }
        return wasRemoved
    }

    override fun isEmpty(): Boolean
    {
        return size == 0
    }

    override fun isEmptyForType(queueTypeProvider: QueueTypeProvider): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueTypeProvider)
        return queueForType.isEmpty()
    }

    override fun pollForType(queueTypeProvider: QueueTypeProvider): QueueMessage?
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueTypeProvider)
        val head = queueForType.poll()
        if (head != null)
        {
            size--
        }
        return head
    }

    override fun peekForType(queueTypeProvider: QueueTypeProvider): QueueMessage?
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueTypeProvider)
        return queueForType.peek()
    }

    override fun containsAll(elements: Collection<QueueMessage>): Boolean
    {
        return elements.stream().allMatch{ element -> this.contains(element) }
    }

    override fun contains(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        return queueForType.contains(element)
    }
}
