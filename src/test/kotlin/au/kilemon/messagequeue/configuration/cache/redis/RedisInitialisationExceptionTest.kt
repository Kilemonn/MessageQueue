package au.kilemon.messagequeue.configuration.cache.redis

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.RuntimeException

/**
 * A unit test class for the [RedisInitialisationException] for any specific tests related to this exception.
 *
 * @author github.com/KyleGonzalez
 */
class RedisInitialisationExceptionTest
{
    /**
     * Ensure that [RedisInitialisationException] is a type of [RuntimeException].
     * Incase this is changed in the future.
     */
    @Test
    fun testTypeOfExceptionIsRuntime()
    {
        val e = RedisInitialisationException("exception message")
        Assertions.assertTrue(RuntimeException::class.isInstance(e))
    }
}
