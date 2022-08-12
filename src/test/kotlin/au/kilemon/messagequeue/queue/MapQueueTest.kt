package au.kilemon.messagequeue.queue

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
 * A test class for the [MapQueue] Component class.
 *
 * @author github.com/KyleGonzalez
 */
@SpringBootTest
class MapQueueTest
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
            open fun getQueue(): MapQueue
            {
                return MapQueue()
            }
        }
    }

    @Autowired
    lateinit var mapQueue: MapQueue

    /**
     * Ensure that when a new entry is added, that the [MapQueue] is no longer empty and reports the correct size.
     */
    @Test
    fun testAdd()
    {
        Assertions.assertTrue(mapQueue.isEmpty())
        val type = QueueType("type")
        val data = Integer(12345)
        val message = QueueMessage(data, type)
        Assertions.assertTrue(mapQueue.add(message))
        Assertions.assertFalse(mapQueue.isEmpty())
        Assertions.assertEquals(1, mapQueue.size)

        val retrievedMessage = mapQueue.pollForType(type)
        Assertions.assertTrue(mapQueue.isEmpty())
        Assertions.assertEquals(0, mapQueue.size)

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
                    mapQueue.peek()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    mapQueue.offer(QueueMessage(Payload("test data"), QueueType("test type")))
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    mapQueue.element()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    mapQueue.poll()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    mapQueue.remove()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    mapQueue.iterator()
                }
            }
        )
    }
}
