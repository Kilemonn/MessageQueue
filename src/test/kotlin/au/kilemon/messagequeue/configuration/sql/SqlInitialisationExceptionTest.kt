package au.kilemon.messagequeue.configuration.sql

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.RuntimeException

/**
 * A unit test class for the [SqlInitialisationException] for any specific tests related to this exception.
 *
 * @author github.com/Kilemonn
 */
class SqlInitialisationExceptionTest
{
    /**
     * Ensure that [SqlInitialisationException] is a type of [RuntimeException].
     * Incase this is changed in future.
     */
    @Test
    fun testTypeOfExceptionIsRuntime()
    {
        val e = SqlInitialisationException("exception message")
        Assertions.assertTrue(RuntimeException::class.isInstance(e))
    }
}
