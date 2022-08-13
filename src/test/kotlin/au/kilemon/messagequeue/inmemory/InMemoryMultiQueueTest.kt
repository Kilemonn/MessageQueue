package au.kilemon.messagequeue.inmemory

import au.kilemon.messagequeue.Payload
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.type.QueueType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * A test class for the [InMemoryMultiQueue] Component class.
 *
 * @author github.com/KyleGonzalez
 */
@SpringBootTest
class InMemoryMultiQueueTest
{
    companion object
    {
        /**
         * A Spring configuration that is used for this test class.
         *
         * @author github.com/KyleGonzalez
         */
        @TestConfiguration
        open class TestContextConfiguration
        {
            @Bean
            open fun getQueue(): InMemoryMultiQueue
            {
                return InMemoryMultiQueue()
            }
        }
    }

    @Autowired
    lateinit var inMemoryMultiQueue: InMemoryMultiQueue

    /**
     * Ensure that when a new entry is added, that the [InMemoryMultiQueue] is no longer empty and reports the correct size.
     */
    @Test
    fun testAdd()
    {
        Assertions.assertTrue(inMemoryMultiQueue.isEmpty())
        val type = QueueType("type")
        val data = Integer(12345)
        val message = QueueMessage(data, type)
        Assertions.assertTrue(inMemoryMultiQueue.add(message))
        Assertions.assertFalse(inMemoryMultiQueue.isEmpty())
        Assertions.assertEquals(1, inMemoryMultiQueue.size)

        val retrievedMessage = inMemoryMultiQueue.pollForType(type)
        Assertions.assertTrue(inMemoryMultiQueue.isEmpty())
        Assertions.assertEquals(0, inMemoryMultiQueue.size)

        Assertions.assertNotNull(retrievedMessage)
        Assertions.assertEquals(data, retrievedMessage!!.data)
    }

    /**
     * Ensure that all applicable methods throw an [UnsupportedOperationException].
     */
    @Test
    fun testUnsupportedMethods()
    {
        assertAll( "Unsupported methods",
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    inMemoryMultiQueue.peek()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    inMemoryMultiQueue.offer(QueueMessage(Payload("test data"), QueueType("test type")))
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    inMemoryMultiQueue.element()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    inMemoryMultiQueue.poll()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    inMemoryMultiQueue.remove()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    inMemoryMultiQueue.iterator()
                }
            }
        )
    }
}
