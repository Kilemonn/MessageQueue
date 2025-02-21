package au.kilemon.messagequeue.queue.cache.memcached

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import au.kilemon.messagequeue.settings.MessageQueueSettings
import net.rubyeye.xmemcached.MemcachedClient
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import java.io.IOException
import java.util.HashSet
import java.util.Optional
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * A `Memcached` specific implementation of the [MultiQueue].
 * All messages stored and accessed directly from the `Memcached` instance.
 * This increases overhead when checking UUID, but it is required incase the cache is edited manually, or by another message managing instance.
 *
 * @author github.com/Kilemonn
 */
class MemcachedMultiQueue(private val prefix: String = ""): MultiQueue(), HasLogger
{
    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    private lateinit var client: MemcachedClient

    /**
     * Getting the size from the underlying cache stats.
     */
    override val size: Int
        get() {
            val stats = client.stats
            //return client.stats.values[0]["curr_items"].toInt()
            return 0
        }

    /**
     * Append the [MessageQueueSettings.cachePrefix] to the provided [subQueue] [String].
     *
     * @param subQueue the [String] to add the prefix to
     * @return a [String] with the provided [subQueue] with the [MessageQueueSettings.cachePrefix] appended to the beginning.
     */
    private fun appendPrefix(subQueue: String): String
    {
        if (hasPrefix() && !subQueue.startsWith(getPrefix()))
        {
            return "${getPrefix()}$subQueue"
        }
        return subQueue
    }

    /**
     * @return whether the [prefix] is [String.isNotBlank]
     */
    internal fun hasPrefix(): Boolean
    {
        return getPrefix().isNotBlank()
    }

    /**
     * @return [prefix]
     */
    internal fun getPrefix(): String
    {
        return prefix
    }

    override fun getNextSubQueueIndex(subQueue: String): Optional<Long>
    {
        val queue = getSubQueue(appendPrefix(subQueue))
        return if (queue.isNotEmpty())
        {
            var lastIndex = queue.last().id
            if (lastIndex == null)
            {
                LOG.warn("subQueue [{}] is not empty but last index is null. Returning index with value [{}].", subQueue, 1)
                return Optional.of(1)
            }
            else
            {
                lastIndex++
                LOG.trace("Incrementing and returning index for subQueue [{}]. Returning index with value [{}].", subQueue, lastIndex)
                return Optional.of(lastIndex)
            }
        }
        else
        {
            LOG.trace("subQueue [{}] is empty, returning index with value [{}].", subQueue, 1)
            Optional.of(1)
        }
    }

    override fun persistMessageInternal(message: QueueMessage)
    {
        val queue = getSubQueue(message.subQueue)
        val matchingMessage = queue.stream().filter{ element -> element.uuid == message.uuid }.findFirst()
        if (matchingMessage.isPresent)
        {
            message.id = matchingMessage.get().id
            val wasRemoved = removeInternal(matchingMessage.get())
            val wasReAdded = addInternal(message)
            if (wasRemoved && wasReAdded)
            {
                return
            }
        }
        throw MessageUpdateException(message.uuid)
    }

    override fun getSubQueueInternal(subQueue: String): Queue<QueueMessage>
    {
        var queue: Queue<QueueMessage>? = client.get<Queue<QueueMessage>?>(appendPrefix(subQueue))
        if (queue == null)
        {
            queue = ConcurrentLinkedQueue<QueueMessage>()
            client.set(appendPrefix(subQueue), 0, queue)
        }

        return queue
    }

    override fun performHealthCheckInternal()
    {
        client.get<Any?>("health-check-key")
    }

    override fun getMessageByUUID(uuid: String): Optional<QueueMessage>
    {
        val subQueue = containsUUID(uuid)
        if (subQueue.isPresent)
        {
            LOG.trace("Found message with uuid [{}].", uuid)
            val queue: Queue<QueueMessage> = getSubQueue(subQueue.get())
            return queue.stream().filter { message -> message.uuid == uuid }.findFirst()
        }
        LOG.trace("No message found with uuid [{}].", uuid)
        return Optional.empty()
    }

    override fun clearSubQueueInternal(subQueue: String): Int
    {
        var amountRemoved = 0
        val queue = getSubQueue(subQueue)
        if (queue.isNotEmpty())
        {
            amountRemoved = queue.size
            client.delete(appendPrefix(subQueue))
            LOG.debug("Cleared existing sub-queue [{}]. Removed [{}] message entries.", subQueue, amountRemoved)
        }
        else
        {
            LOG.debug("Attempting to clear non-existent sub-queue [{}]. No messages cleared.", subQueue)
        }
        return amountRemoved
    }

    override fun isEmptySubQueue(subQueue: String): Boolean
    {
        return getSubQueue(subQueue).isEmpty()
    }

    override fun pollInternal(subQueue: String): Optional<QueueMessage>
    {
        val queue = getSubQueue(subQueue)
        if (queue.isNotEmpty())
        {
            return Optional.of(queue.iterator().next())
        }
        return Optional.empty()
    }

    override fun keysInternal(includeEmpty: Boolean): HashSet<String>
    {
        // TODO
        try
        {
            // TODO: Add "wsl -- " only for windows
            val command = "wsl --% sh -c \"echo \"\"stats cachedump 10 0\"\" | nc ${client.availableServers.first().address.toString().split("/")[0]} ${client.availableServers.first().port}\""
            // val parts = command.split("\\s".toRegex())
            val proc = ProcessBuilder(command)
                .start()

            proc.waitFor(2, TimeUnit.SECONDS)
            val result = proc.inputStream.bufferedReader().readText()
            println(result)
        }
        catch(e: IOException)
        {
            e.printStackTrace()
        }
        return HashSet()
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        for (key in keys())
        {
            val queue = getSubQueue(key)
            val anyMatchTheUUID = queue.stream().anyMatch{ message -> uuid == message.uuid }
            if (anyMatchTheUUID)
            {
                LOG.debug("Found sub-queue [{}] for message UUID: [{}].", key, uuid)
                return Optional.of(key)
            }
        }
        LOG.debug("No sub-queue contains message with UUID: [{}].", uuid)
        return Optional.empty()
    }

    override fun addInternal(element: QueueMessage): Boolean
    {
        val queue: Queue<QueueMessage> = getSubQueue(element.subQueue)
        val wasAdded = queue.add(element)
        return wasAdded && client.set(appendPrefix(element.subQueue), 0, queue)
    }

    override fun removeInternal(element: QueueMessage): Boolean
    {
        val queue: Queue<QueueMessage> = getSubQueue(element.subQueue)
        val wasRemoved = queue.remove(element)
        return wasRemoved && client.set(appendPrefix(element.subQueue), 0, queue)
    }
}
