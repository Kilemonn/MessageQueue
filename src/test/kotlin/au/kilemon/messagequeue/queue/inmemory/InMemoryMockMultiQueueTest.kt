package au.kilemon.messagequeue.queue.inmemory

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.exception.HealthCheckFailureException
import au.kilemon.mockall.MockAllExecutionListener
import au.kilemon.mockall.NotMocked
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

/**
 * A [Mockito] test that is used to simulate hard to cover error cases in calling code for all `MultiQueue` related methods that are hard to test.
 */
@ExtendWith(SpringExtension::class)
@TestExecutionListeners(MockAllExecutionListener::class)
class InMemoryMockMultiQueueTest
{
    @NotMocked([InMemoryMultiQueue::class])
    private lateinit var multiQueue: InMemoryMultiQueue

    @BeforeEach
    fun setup()
    {
        multiQueue.clear()
    }

    /**
     * Test [InMemoryMultiQueue.add] to ensure that `false` is returned when [InMemoryMultiQueue.addInternal] returns `false`.
     */
    @Test
    fun testPerformAdd_returnsFalse()
    {
        val message = QueueMessage(null, "testPerformAdd_returnsFalse")
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

    /**
     * Test [InMemoryMultiQueue.retainAll] and test a specific scenario where we think the entry exists in the queue and
     * attempt to remove it, but fail to remove it from the queue.
     */
    @Test
    fun testRetainAll_removeFails()
    {
        val message = QueueMessage("payload", "testRetainAll_removeFails")
        Mockito.`when`(multiQueue.remove(message)).thenReturn(false)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.retainAll(Collections.emptyList()))
    }
}
