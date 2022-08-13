package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * A test class for the [InMemoryMultiQueue] Component class.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@Import(InMemoryTestConfiguration::class)
class InMemoryMultiQueueTest: AbstractMultiQueueTest<InMemoryMultiQueue>()
{
    override fun duringSetup()
    {

    }
}
