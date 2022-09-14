package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import lombok.extern.slf4j.Slf4j
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The InMemoryMultiQueue which implements the [MultiQueue]. It holds a [ConcurrentHashMap] with [Queue] entries.
 * Using the provided [String], specific entries in the queue can be manipulated and changed as needed.
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

    /**
     * An internal [Map] that holds known [UUID]s (as a [String]) and their related `queueType` to quickly find entries within the [MultiQueue].
     */
    private val uuidMap: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    override var size: Int = 0

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        var queueForType: Queue<QueueMessage>? = messageQueue[queueType]
        if (queueForType == null)
        {
            queueForType = ConcurrentLinkedQueue()
            this.initialiseQueueForType(queueType, queueForType)
        }
        return queueForType
    }

    override fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>)
    {
        messageQueue[queueType] = queue
    }

    override fun add(element: QueueMessage): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(element.type)
        if ( !containsUUID(element.uuid).isPresent)
        {
            val wasAdded = queueForType.add(element)
            if (wasAdded)
            {
                uuidMap[element.uuid.toString()] = element.type
                size++
                return true
            }
        }
        return false
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
        uuidMap.clear()
        size = 0
    }

    override fun clearForType(queueType: String)
    {
        val queueForType: Queue<QueueMessage>? = messageQueue[queueType]
        if (queueForType != null)
        {
            size -= queueForType.size
            queueForType.forEach { message -> uuidMap.remove(message.uuid.toString()) }
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
                    if ( !elements.contains(entry))
                    {
                        val wasRemoved = queueForKey.remove(entry)
                        anyWasRemoved = wasRemoved || anyWasRemoved
                        if (wasRemoved)
                        {
                            uuidMap.remove(entry.uuid.toString())
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
            uuidMap.remove(element.uuid.toString())
            size--
        }
        return wasRemoved
    }

    override fun isEmpty(): Boolean
    {
        return size == 0
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        return queueForType.isEmpty()
    }

    override fun pollForType(queueType: String): Optional<QueueMessage>
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        val head = queueForType.poll()
        if (head != null)
        {
            uuidMap.remove(head.uuid.toString())
            size--
        }
        return Optional.ofNullable(head)
    }

    override fun peekForType(queueType: String): Optional<QueueMessage>
    {
        val queueForType: Queue<QueueMessage> = getQueueForType(queueType)
        return Optional.ofNullable(queueForType.peek())
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

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        if (includeEmpty)
        {
            return messageQueue.keys.toSet()
        }
        else
        {
            val keys = HashSet<String>()
            for (key: String in messageQueue.keys)
            {
                val queueForType = getQueueForType(key)
                if (queueForType.isNotEmpty())
                {
                    keys.add(key)
                }
            }
            return keys
        }
    }

    override fun containsUUID(uuid: UUID): Optional<String>
    {
        return Optional.ofNullable(uuidMap[uuid.toString()])
    }
}
