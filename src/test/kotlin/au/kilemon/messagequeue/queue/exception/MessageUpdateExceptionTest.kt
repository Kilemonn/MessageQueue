package au.kilemon.messagequeue.queue.exception

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * A unit test class for the [MessageUpdateException] for any specific tests related to this exception.
 *
 * @author github.com/Kilemonn
 */
class MessageUpdateExceptionTest
{
    /**
     * Ensure that [MessageUpdateException] is a type of [Exception] and not [RuntimeException].
     * Incase this is changed in the future.
     */
    @Test
    fun testTypeOfException()
    {
        val e = MessageUpdateException("uuid")
        Assertions.assertTrue(Exception::class.isInstance(e))
        Assertions.assertFalse(RuntimeException::class.isInstance(e))
    }
}
