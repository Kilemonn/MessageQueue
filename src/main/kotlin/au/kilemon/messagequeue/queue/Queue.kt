package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.message.Message
import au.kilemon.messagequeue.message.MessageType
import au.kilemon.messagequeue.message.MultiQueue
import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap

/**
 *
 *
 * @author github.com/KyleGonzalez
 */
@Slf4j
@Component
open class Queue<T: Serializable> : MultiQueue<Message<T>>
{
    private val messageQueue: ConcurrentHashMap<String, Queue<Message<T>>> = ConcurrentHashMap()

    override var size: Int = 0

    private fun getQueueForType(messageType: MessageType): Queue<Message<T>>
    {
        var queueForType: Queue<Message<T>>? = messageQueue[messageType.getIdentifier()]
        if (queueForType == null)
        {
            queueForType = LinkedList()
        }
        return queueForType
    }

    override fun add(element: Message<T>): Boolean
    {
        val queueForType: Queue<Message<T>> = getQueueForType(element.type)
        val wasAdded = queueForType.add(element)
        if (wasAdded)
        {
            size++
        }
        return wasAdded
    }

    override fun addAll(elements: Collection<Message<T>>): Boolean
    {
        var wasAdded = false
        for (element: Message<T> in elements)
        {
            wasAdded = wasAdded || add(element)
        }
        return wasAdded
    }

    override fun clear()
    {
        messageQueue.clear()
        size = 0
    }

    override fun clearForType(messageType: MessageType)
    {
        val queueForType: Queue<Message<T>>? = messageQueue[messageType.getIdentifier()]
        if (queueForType != null)
        {
            size -= queueForType.size
            queueForType.clear()
        }
    }

    override fun retainAll(elements: Collection<Message<T>>): Boolean
    {
        var anyWasRemoved = false
        for (key: String in messageQueue.keys())
        {
            val queueForKey: Queue<Message<T>>? = messageQueue[key]
            if (queueForKey != null)
            {
                for(entry: Message<T> in queueForKey)
                {
                    if (!elements.contains(entry))
                    {
                        anyWasRemoved = anyWasRemoved || queueForKey.remove(entry)
                    }
                }
            }
        }
        return anyWasRemoved
    }

    override fun removeAll(elements: Collection<Message<T>>): Boolean
    {
        var wasRemoved = false
        for (element: Message<T> in elements)
        {
            wasRemoved = wasRemoved || remove(element)
        }
        return wasRemoved
    }

    override fun remove(element: Message<T>): Boolean
    {
        val queueForType: Queue<Message<T>> = getQueueForType(element.type)
        val wasRemoved = queueForType.remove(element)
        if (wasRemoved)
        {
            size--
        }
        return wasRemoved
    }

    override fun isEmpty(): Boolean
    {
        return messageQueue.isEmpty()
    }

    override fun isEmptyForType(messageType: MessageType): Boolean
    {
        val queueForType: Queue<Message<T>> = getQueueForType(messageType)
        return queueForType.isEmpty()
    }

    override fun pollForType(messageType: MessageType): Message<T>?
    {
        val queueForType: Queue<Message<T>> = getQueueForType(messageType)
        val head = queueForType.poll()
        if (head != null)
        {
            size--
        }
        return head
    }

    override fun peekForType(messageType: MessageType): Message<T>?
    {
        val queueForType: Queue<Message<T>> = getQueueForType(messageType)
        return queueForType.peek()
    }

    override fun containsAll(elements: Collection<Message<T>>): Boolean
    {
        var allContained = true
        for (element: Message<T> in elements)
        {
            allContained = allContained && contains(element)
        }
        return allContained
    }

    override fun contains(element: Message<T>): Boolean
    {
        val queueForType: Queue<Message<T>> = getQueueForType(element.type)
        return queueForType.contains(element)
    }
}
