package au.kilemon.messagequeue.authentication.exception

import au.kilemon.messagequeue.queue.exception.DuplicateMessageException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * A unit test class for the [MultiQueueAuthenticationException] for any specific tests related to this exception.
 *
 * @author github.com/Kilemonn
 */
class MultiQueueAuthenticationExceptionTest
{
    /**
     * Ensure that [MultiQueueAuthenticationException] is a type of [Exception] and not [RuntimeException].
     * Incase this is changed in future.
     */
    @Test
    fun testTypeOfException()
    {
        val e = MultiQueueAuthenticationException()
        Assertions.assertTrue(Exception::class.isInstance(e))
        Assertions.assertFalse(RuntimeException::class.isInstance(e))
    }
}
