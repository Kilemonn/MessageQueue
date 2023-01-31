package au.kilemon.messagequeue.queue.exception

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * A unit test class for the [HealthCheckFailureException] for any specific tests related to this exception.
 *
 * @author github.com/KyleGonzalez
 */
class HealthCheckFailureExceptionTest
{
    /**
     * Ensure that [HealthCheckFailureException] is a type of [Exception] and not a [RuntimeException].
     * Incase this is changed in the future.
     */
    @Test
    fun testTypeOfException()
    {
        val e = HealthCheckFailureException("error message", null)
        Assertions.assertTrue(Exception::class.isInstance(e))
        Assertions.assertFalse(RuntimeException::class.isInstance(e))
    }
}
