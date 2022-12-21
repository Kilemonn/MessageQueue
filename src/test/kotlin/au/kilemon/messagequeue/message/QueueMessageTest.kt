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

    /**
     * Ensure that two [QueueMessage]s are not equal if one has `null` [QueueMessage.payload] and [QueueMessage.payloadBytes], but the same
     * [QueueMessage.uuid] and [QueueMessage.type].
     */
    @Test
    fun testEquals_withOneMessageHavingNullPayloadAndBytes()
    {
        val uuid = UUID.randomUUID().toString()
        val type = "type"
        val message1 = QueueMessage()
        message1.payload = null
        message1.uuid = uuid
        message1.type = type
        val message2 = QueueMessage(payload = "stuff", type = type)
        message2.uuid = uuid

        Assertions.assertNull(message1.payload)
        Assertions.assertNull(message1.payloadBytes)
        Assertions.assertNotNull(message2.payload)
        Assertions.assertNotNull(message2.payloadBytes)
        Assertions.assertNotEquals(message1, message2)
    }

    /**
     * Ensure that two [QueueMessage] are equal if they both have `null` [QueueMessage.payload] but `equal` [QueueMessage.payloadBytes], and the same
     * [QueueMessage.uuid] and [QueueMessage.type].
     */
    @Test
    fun testEquals_withEqualPayloadBytes()
    {
        val uuid = UUID.randomUUID().toString()
        val type = "type"
        val message1 = QueueMessage(payload = "stuff", type = type)
        message1.payload = null
        message1.uuid = uuid
        val message2 = QueueMessage(payload = "stuff", type = type)
        message2.payload = null
        message2.uuid = uuid

        Assertions.assertTrue(message1.payloadBytes.contentEquals(message2.payloadBytes))
        Assertions.assertEquals(message1, message2)
    }

    /**
     * Ensure that two [QueueMessage] are equal if they both have `null` [QueueMessage.payloadBytes] but `equal` [QueueMessage.payload], and the same
     * [QueueMessage.uuid] and [QueueMessage.type].
     */
    @Test
    fun testEquals_withEqualPayloads()
    {
        val uuid = UUID.randomUUID().toString()
        val type = "type"
        val message1 = QueueMessage()
        message1.uuid = uuid
        message1.type = type
        val message2 = QueueMessage()
        message2.uuid = uuid
        message2.type = type

        // Set the payload into both objects to ensure this is considered in the equals check too
        val obj = 1287354
        message1.payload = obj
        message2.payload = obj

        Assertions.assertNull(message1.payloadBytes)
        Assertions.assertNull(message2.payloadBytes)
        Assertions.assertEquals(message1.payload, message2.payload)
        Assertions.assertEquals(message1, message2)
    }

    /**
     * Ensure that two [QueueMessage] are equal if they both have `equal` [QueueMessage.payloadBytes] and [QueueMessage.payload], and the same
     * [QueueMessage.uuid] and [QueueMessage.type].
     */
    @Test
    fun testEquals_withEqualPayloadsAndBytes()
    {
        val uuid = UUID.randomUUID().toString()
        val type = "type"
        val message1 = QueueMessage(payload = "stuff", type = type)
        message1.uuid = uuid
        val message2 = QueueMessage(payload = "stuff", type = type)
        message2.uuid = uuid

        Assertions.assertEquals(message1.payload, message2.payload)
        Assertions.assertTrue(message1.payloadBytes.contentEquals(message2.payloadBytes))
        Assertions.assertEquals(message1, message2)
    }
}
