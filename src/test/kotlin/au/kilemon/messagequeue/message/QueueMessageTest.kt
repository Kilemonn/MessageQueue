package au.kilemon.messagequeue.message

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

/**
 * A test class for the [QueueMessage].
 * Specifically to verify the [equals] and [hashCode] methods and how they interact with containers and other things.
 *
 * @author github.com/KyleGonzalez
 */
class QueueMessageTest
{
    /**
     * Ensure that two [QueueMessage] with the same appropriate properties are `equal` when the [equals] method is called on them.
     */
    @Test
    fun testEquals()
    {
        val uuid = UUID.randomUUID().toString()
        val message1 = QueueMessage()
        message1.uuid = uuid
        val message2 = QueueMessage()
        message2.uuid = uuid

        Assertions.assertEquals(message1, message1)
        Assertions.assertEquals(message1, message2)
        Assertions.assertEquals(message2, message1)
        Assertions.assertEquals(message2, message2)
    }
}
