package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * A test class for the [InMemoryMultiQueue] Component class.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, AbstractMultiQueueTest.AbstractMultiQueueTestConfiguration::class] )
class InMemoryMultiQueueTest: AbstractMultiQueueTest()
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
