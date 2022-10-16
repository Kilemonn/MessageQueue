package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import au.kilemon.messagequeue.queue.MultiQueue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * A test class for the [InMemoryMultiQueue] Component class.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
class InMemoryMultiQueueTest: AbstractMultiQueueTest<InMemoryMultiQueue>()
{
    /**
     * A Spring configuration that is used for this test class.
     *
     * This is specifically creating the [InMemoryMultiQueue] to be autowired in the parent
     * class and used in all the tests.
     *
     * @author github.com/KyleGonzalez
     */
    @TestConfiguration
    internal class InMemoryTestConfiguration
    {
        @Bean
        open fun getInMemoryMultiQueue(): InMemoryMultiQueue
        {
            return InMemoryMultiQueue()
        }
    }

    /**
     * Ensure the [MultiQueue] is cleared before each test.
     */
    @BeforeEach
    fun setup()
    {
        multiQueue.clear()
    }
}
