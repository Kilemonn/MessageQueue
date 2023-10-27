package au.kilemon.messagequeue.authentication

import au.kilemon.messagequeue.message.QueueMessage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

/**
 * A test class for the [AuthenticationMatrix].
 * Specifically to verify the [equals] method.
 *
 * @author github.com/Kilemonn
 */
class AuthenticationMatrixTest
{
    /**
     * Ensure that two [AuthenticationMatrix] with the same appropriate properties are `equal` when the [equals]
     * method is called on them.
     */
    @Test
    fun testEquals()
    {
        val authMatrix1 = AuthenticationMatrix("authMatrix")
        val authMatrix2 = AuthenticationMatrix("authMatrix")
        val authMatrix3 = AuthenticationMatrix("authMatrix3")

        Assertions.assertEquals(authMatrix1, authMatrix1)
        Assertions.assertEquals(authMatrix1, authMatrix2)
        Assertions.assertEquals(authMatrix2, authMatrix1)
        Assertions.assertEquals(authMatrix2, authMatrix2)

        Assertions.assertNotEquals(authMatrix1, authMatrix3)
        Assertions.assertNotEquals(authMatrix2, authMatrix3)
    }
}
