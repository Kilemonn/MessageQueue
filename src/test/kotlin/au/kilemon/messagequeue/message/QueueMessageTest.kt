package au.kilemon.messagequeue.message

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.util.SerializationUtils
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

        Assertions.assertArrayEquals(message1.payloadBytes, message2.payloadBytes)
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
        Assertions.assertArrayEquals(message1.payloadBytes, message2.payloadBytes)
        Assertions.assertEquals(message1, message2)
    }

    /**
     * Ensure that [QueueMessage.equals] returns `false` when all properties are equal except [QueueMessage.type].
     */
    @Test
    fun testEquals_nonEqualType()
    {
        val uuid = UUID.randomUUID().toString()
        val message1 = QueueMessage(payload = "stuff", type = "type1")
        message1.uuid = uuid
        val message2 = QueueMessage(payload = "stuff", type = "type2")
        message2.uuid = uuid

        Assertions.assertEquals(message1.payload, message2.payload)
        Assertions.assertArrayEquals(message1.payloadBytes, message2.payloadBytes)
        Assertions.assertNotEquals(message1, message2)
    }

    /**
     * Ensure that `false` is returned when [QueueMessage] is equated against `null`.
     */
    @Test
    fun testEquals_withNull()
    {
        val message = QueueMessage(payload = "data", type = "testEquals_withNull")
        Assertions.assertNotEquals(message, null)
    }

    /**
     * Ensure that `false` is returned when [QueueMessage] is equated against an object that is not a [QueueMessage].
     */
    @Test
    fun testEquals_withNonQueueMessageObject()
    {
        val message = QueueMessage(payload = "data", type = "testEquals_withNonQueueMessageObject")
        val obj = Any()
        Assertions.assertTrue(obj !is QueueMessage)
        Assertions.assertNotEquals(message, obj)
    }

    /**
     * Ensure that the payload object is not changed if the underlying [QueueMessage.payloadBytes] is null, and [QueueMessage.payload] is not null when the [QueueMessage.resolvePayloadObject] is called.
     * Since [QueueMessage.payload] is `not-null` it should not be changed.
     */
    @Test
    fun testResolvePayload_payloadNotNullBytesNull()
    {
        val payload = "testResolvePayload_payloadNotNullBytesNull"
        val message = QueueMessage(null, type = "test")
        Assertions.assertNull(message.payloadBytes)
        message.payload = payload
        message.resolvePayloadObject()
        Assertions.assertEquals(payload, message.payload)
    }

    /**
     * Ensure that the payload object is not changed if the underlying [QueueMessage.payloadBytes] is non-null, and [QueueMessage.payload] is not null when the [QueueMessage.resolvePayloadObject] is called.
     * Since [QueueMessage.payload] is `not-null` it should not be changed.
     */
    @Test
    fun testResolvePayload_payloadNotNullBytesNotNull()
    {
        val payload = "testResolvePayload_payloadNotNullBytesNotNull"
        val payloadBytes = "payload-bytes"
        val message = QueueMessage(payloadBytes, type = "test")
        message.payload = payload
        Assertions.assertEquals(payload, message.payload)
        Assertions.assertArrayEquals(SerializationUtils.serialize(payloadBytes), message.payloadBytes)
        message.resolvePayloadObject()
        Assertions.assertEquals(payload, message.payload)
        Assertions.assertArrayEquals(SerializationUtils.serialize(payloadBytes), message.payloadBytes)
    }

    /**
     * Ensure that the payload object is not changed if the underlying [QueueMessage.payloadBytes] is null, and [QueueMessage.payload] is null when the [QueueMessage.resolvePayloadObject] is called.
     * Since [QueueMessage.payloadBytes] is `null` it should not try to update the [QueueMessage.payload].
     */
    @Test
    fun testResolvePayload_payloadNullBytesNull()
    {
        val message = QueueMessage(null, type = "test")

        Assertions.assertNull(message.payload)
        Assertions.assertNull(message.payloadBytes)
        message.resolvePayloadObject()
        Assertions.assertNull(message.payload)
        Assertions.assertNull(message.payloadBytes)
    }

    /**
     * Ensure that the payload object is not changed if the underlying [QueueMessage.payloadBytes] is non-null, and [QueueMessage.payload] is null when the [QueueMessage.resolvePayloadObject] is called.
     * Since [QueueMessage.payloadBytes] is `non-null` it should deserialise and resolve the [QueueMessage.payload] to an object.
     * This is the most common scenario that this method is resolving.
     */
    @Test
    fun testResolvePayload_payloadNullBytesNotNull()
    {
        val payload = "testResolvePayload_payloadNullBytesNotNull"
        val message = QueueMessage(payload, type = "test")

        // At this point the payload property will quest payloadBytes, we need to overwrite
        message.payload = null
        Assertions.assertArrayEquals(SerializationUtils.serialize(payload), message.payloadBytes)
        Assertions.assertNull(message.payload)
        message.resolvePayloadObject()
        Assertions.assertArrayEquals(SerializationUtils.serialize(payload), message.payloadBytes)
        Assertions.assertEquals(payload, message.payload)
    }
}
