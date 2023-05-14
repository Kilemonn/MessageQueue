package au.kilemon.messagequeue.queue.exception

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


/**
 * A unit test class for the [DuplicateMessageException] for any specific tests related to this exception.
 *
 * @author github.com/Kilemonn
 */
class DuplicateMessageExceptionTest
{
    /**
     * Ensure that [DuplicateMessageException] is a type of [Exception] and not [RuntimeException].
     * Incase this is changed in future.
     */
    @Test
    fun testTypeOfException()
    {
        val e = DuplicateMessageException("uuid", "type")
        Assertions.assertTrue(Exception::class.isInstance(e))
        Assertions.assertFalse(RuntimeException::class.isInstance(e))
    }
}
