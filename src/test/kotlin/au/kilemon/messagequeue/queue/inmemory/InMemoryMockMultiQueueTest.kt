package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.exception.HealthCheckFailureException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

/**
 * A [Mockito] test that is used to simulate hard to cover error cases in calling code for all `MultiQueue` related methods that are hard to test.
 */
@ExtendWith(SpringExtension::class)
class InMemoryMockMultiQueueTest
{
    private val multiQueue: InMemoryMultiQueue = Mockito.spy(InMemoryMultiQueue::class.java)

    @BeforeEach
    fun setUp()
    {
        multiQueue.initialiseQueueIndex()
    }

    /**
     * Test [InMemoryMultiQueue.add] to ensure that `false` is returned when [InMemoryMultiQueue.addInternal] returns `false`.
     */
    @Test
    fun testPerformAdd_returnsFalse()
    {
        val message = QueueMessage(null, "type")
        Mockito.`when`(multiQueue.addInternal(message)).thenReturn(false)
        Mockito.`when`(multiQueue.containsUUID(message.uuid)).thenReturn(Optional.empty())
        Assertions.assertFalse(multiQueue.add(message))
    }

    /**
     * Test [InMemoryMultiQueue.performHealthCheck] to ensure [HealthCheckFailureException] is thrown from [InMemoryMultiQueue.performHealthCheck] when
     * [InMemoryMultiQueue.performHealthCheckInternal] throws an exception.
     */
    @Test
    fun testPerformHealthCheck_throws()
    {
        val wrappedException = RuntimeException("Wrapped!")
        Mockito.`when`(multiQueue.performHealthCheckInternal()).thenThrow(wrappedException)
        val thrown = Assertions.assertThrows(HealthCheckFailureException::class.java) {
            multiQueue.performHealthCheck()
        }
        Assertions.assertEquals(wrappedException, thrown.cause)
    }
}
