package au.kilemon.messagequeue.queue.cache

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.queue.exception.IllegalSubQueueIdentifierException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.IntStream
import kotlin.test.Test

/**
 * A test to verify the [CacheKeyManager] specific functionality within the [CacheMultiQueue].
 *
 * @author github.com/Kilemonn
 */
abstract class CacheMultiQueueTest: MultiQueueTest()
{
    @Autowired
    protected lateinit var keyManager: CacheKeyManager

    @BeforeEach
    open fun beforeEach()
    {
        Assertions.assertTrue(multiQueue is CacheMultiQueue)
        keyManager.clear()
    }

    /**
     * Ensure that sub queue identifiers are added correctly to [CacheKeyManager].
     */
    @Test
    fun testCacheKeyManager_addingSubQueue()
    {
        val subQueue = "testCacheKeyManager_addingSubQueue"
        val message = QueueMessage("data", subQueue)

        Assertions.assertFalse(keyManager.contains(subQueue))
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(keyManager.contains(subQueue))

        // Make sure only 1 sub queue is registered, even though there is 1 hidden sub queue holding the sub queues
        Assertions.assertEquals(1, multiQueue.keys().size)
        Assertions.assertEquals(1, keyManager.getKeys().size)
    }

    /**
     * Ensure we cannot create any entries using the reserved [CacheKeyManager.CACHE_KEYS_KEY] (with prefix).
     */
    @Test
    fun testCacheKeyManager_accessingRestrictedCacheKeyEntry()
    {
        val subQueue = CacheKeyManager.CACHE_KEYS_KEY

        Assertions.assertFalse(keyManager.contains(subQueue))
        val message = QueueMessage("data", subQueue)
        Assertions.assertThrows(IllegalSubQueueIdentifierException::class.java) {
            multiQueue.add(message)
        }
        Assertions.assertFalse(multiQueue.contains(message))
        Assertions.assertFalse(keyManager.contains(subQueue))
    }

    /**
     * Ensure multiple sub queue identifiers can be added to the [CacheKeyManager].
     */
    @Test
    fun testCacheKeyManager_addingMultipleKeys()
    {
        val keysSize = 10
        val keyPrefix = "keys"
        val allSubQueuePrefix = (multiQueue as CacheMultiQueue).getPrefix()
        IntStream.range(0, keysSize).forEach { i ->
            val key = "$keyPrefix$i"
            val message = QueueMessage("data", key)
            Assertions.assertFalse(keyManager.contains(key))
            Assertions.assertTrue(multiQueue.add(message))
            Assertions.assertTrue(keyManager.contains("$allSubQueuePrefix$key"))
        }

        val keys = keyManager.getKeys()
        Assertions.assertEquals(keysSize, keys.size)
    }

    /**
     * Ensure we clear the entry from [CacheKeyManager] when [au.kilemon.messagequeue.queue.MultiQueue.clearSubQueue] is called.
     */
    @Test
    fun testCacheKeyManager_removingKey()
    {
        val subQueue = "testCacheKeyManager_removingKey"
        val message = QueueMessage("data", subQueue)

        Assertions.assertFalse(keyManager.contains(subQueue))
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(keyManager.contains(subQueue))

        Assertions.assertEquals(1, multiQueue.clearSubQueue(subQueue))
        Assertions.assertFalse(keyManager.contains(subQueue))
    }
}
