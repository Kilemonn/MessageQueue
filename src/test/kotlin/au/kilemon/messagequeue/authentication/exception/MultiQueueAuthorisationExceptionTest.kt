package au.kilemon.messagequeue.authentication.exception

import au.kilemon.messagequeue.authentication.RestrictionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * A unit test class for the [MultiQueueAuthorisationException] for any specific tests related to this exception.
 *
 * @author github.com/Kilemonn
 */
class MultiQueueAuthorisationExceptionTest
{
    /**
     * Ensure that [MultiQueueAuthorisationException] is a type of [Exception] and not [RuntimeException].
     * Incase this is changed in future.
     */
    @Test
    fun testTypeOfException()
    {
        val e = MultiQueueAuthorisationException("sub-queue", RestrictionMode.NONE)
        Assertions.assertTrue(Exception::class.isInstance(e))
        Assertions.assertFalse(RuntimeException::class.isInstance(e))
    }
}
