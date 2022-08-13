package au.kilemon.messagequeue.queue.inmemory

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * A Spring configuration that is used for this test class.
 *
 * This is specifically creating the [InMemoryMultiQueue] to be autowired in the parent
 * class and used in all the tests.
 *
 * @author github.com/KyleGonzalez
 */
@TestConfiguration
open class InMemoryTestConfiguration
{
    @Bean
    open fun getInMemoryMultiQueue(): InMemoryMultiQueue
    {
        return InMemoryMultiQueue()
    }
}