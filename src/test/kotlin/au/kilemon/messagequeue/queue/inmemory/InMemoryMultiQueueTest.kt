package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * A test class for the [InMemoryMultiQueue] Component class.
 *
 * @author github.com/KyleGonzalez
 */
class InMemoryMultiQueueTest: AbstractMultiQueueTest<InMemoryMultiQueue>()
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

    override fun duringSetup()
    {

    }
}
