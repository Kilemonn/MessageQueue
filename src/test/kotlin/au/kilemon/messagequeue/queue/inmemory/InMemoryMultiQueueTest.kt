package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.queue.MultiQueue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * A test class for the [InMemoryMultiQueue] Component class.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class InMemoryMultiQueueTest: MultiQueueTest()
{
    /**
     * Ensure the [MultiQueue] is cleared before each test.
     */
    @BeforeEach
    fun setup()
    {
        multiQueue.clear()
    }
}
