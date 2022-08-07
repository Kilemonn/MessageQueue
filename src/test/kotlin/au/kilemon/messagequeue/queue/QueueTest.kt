package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.Payload
import au.kilemon.messagequeue.message.Message
import au.kilemon.messagequeue.message.MessageType
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * A test class for the [Queue] Component class.
 *
 * @author github.com/KyleGonzalez
 */
@SpringBootTest
class QueueTest
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
            open fun getQueue(): Queue<Message<Payload>>
            {
                return Queue()
            }
        }
    }

    @Autowired
    lateinit var queue: Queue<Payload>

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
                    queue.peek()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    queue.offer(Message(Payload("test data"), MessageType("test type")))
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    queue.element()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    queue.poll()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    queue.remove()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    queue.iterator()
                }
            }
        )
    }
}
