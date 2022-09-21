package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.Payload
import au.kilemon.messagequeue.PayloadEnum
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.MessageResponse
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

/**
 * A test class for the [MessageQueueController].
 * Providing tests of the context and endpoint validation/handling itself.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [MessageQueueController::class])
class MessageQueueControllerTest
{
    /**
     * A [TestConfiguration] for the outer [MessageQueueControllerTest] class.
     *
     * @author github.com/KyleGonzalez
     */
    @TestConfiguration
    open class TestConfig
    {
        @Bean
        open fun getSettings(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var multiQueue: MultiQueue

    private val gson: Gson = Gson()

    /**
     * [BeforeEach] method to run [MultiQueue.clear] and ensure that [MultiQueue.isEmpty] returns `true` at the beginning of each test.
     */
    @BeforeEach
    fun setUp()
    {
        multiQueue.clear()
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    @Test
    fun testGetQueueEntry()
    {
        val queueType = "test"
        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_TYPE + "/" + queueType)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("0"))
    }

    /**
     * Test [MessageQueueController.getEntry] to ensure that [HttpStatus.OK] and the correct [QueueMessage] is returned as the response.
     */
    @Test
    fun testGetEntry()
    {
        val message = createQueueMessage()

        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(message.payload, deserialisedPayload)
        Assertions.assertFalse(messageResponse.message.consumed)
        Assertions.assertNull(messageResponse.message.consumedBy)
        Assertions.assertEquals(message.type, messageResponse.message.type)
        Assertions.assertEquals(message.type, messageResponse.queueType)
        Assertions.assertNotNull(messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.getEntry] to ensure that [HttpStatus.NOT_FOUND] is returned when a [UUID] that does not exist is provided.
     */
    @Test
    fun testGetEntry_ResponseBody_NotExists()
    {
        val uuid = "invalid-not-found-uuid"
        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Calling create with provided [QueueMessage.consumedBy], [QueueMessage.consumed] and [QueueMessage.uuid] to
     * ensure they are set correctly in the returned [MessageResponse].
     */
    @Test
    fun testCreateQueueEntry_withProvidedDefaults()
    {
        val message = createQueueMessage(consumedBy = "consumed")

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(message.payload, deserialisedPayload)
        Assertions.assertEquals(message.consumed, messageResponse.message.consumed)
        Assertions.assertEquals(message.consumedBy, messageResponse.message.consumedBy)
        Assertions.assertEquals(message.type, messageResponse.message.type)
        Assertions.assertEquals(message.type, messageResponse.queueType)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)
    }

    /**
     * Calling create without [QueueMessage.consumedBy], [QueueMessage.consumed] and [QueueMessage.uuid] to
     * ensure they are initialised as expected when they are not provided by the caller.
     */
    @Test
    fun testCreateQueueEntry_withOutDefaults()
    {
        val message = createQueueMessage()

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(message.payload, deserialisedPayload)
        Assertions.assertFalse(messageResponse.message.consumed)
        Assertions.assertNull(messageResponse.message.consumedBy)
        Assertions.assertEquals(message.type, messageResponse.message.type)
        Assertions.assertEquals(message.type, messageResponse.queueType)
        Assertions.assertNotNull(messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.createMessage] to ensure that [HttpStatus.CONFLICT] is returned if a message with the same [UUID] already exists in the queue.
     */
    @Test
    fun testCreateEntry_Conflict()
    {
        val message = createQueueMessage()

        Assertions.assertTrue(multiQueue.add(message))

        mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isConflict)
    }

    /**
     * Test [MessageQueueController.getKeys] to ensure that all keys for existing entries are provided and exist within the [MultiQueue].
     */
    @Test
    fun testGetKeys()
    {
        val entries = initialiseMapWithEntries()

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val keys = gson.fromJson(mvcResult.response.contentAsString, List::class.java)
        Assertions.assertFalse(keys.isNullOrEmpty())
        Assertions.assertEquals(entries.second.size, keys.size)
        entries.second.forEach { type -> Assertions.assertTrue(keys.contains(type)) }
    }

    /**
     * Test [MessageQueueController.getKeys] to ensure that all keys are returned. Specifically when entries are added and `includeEmpty` is set to `false`.
     */
    @Test
    fun testGetKeys_excludeEmpty()
    {
        val entries = initialiseMapWithEntries()
        Assertions.assertTrue(multiQueue.remove(entries.first[0]))
        Assertions.assertTrue(multiQueue.remove(entries.first[1]))

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("includeEmpty", "false"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val keys = gson.fromJson(mvcResult.response.contentAsString, List::class.java)
        Assertions.assertFalse(keys.isNullOrEmpty())
        Assertions.assertEquals(2, keys.size)
        entries.second.subList(2, 3).forEach { type -> Assertions.assertTrue(keys.contains(type)) }
    }

    /**
     * Test [MessageQueueController.getAll] to ensure that all entries are returned from all `queueTypes` when an explicit `queueType` is not provided.
     */
    @Test
    fun testGetAll()
    {
        val entries = initialiseMapWithEntries()

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ALL)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
        val keys = gson.fromJson<Map<String, List<String>>>(mvcResult.response.contentAsString, mapType)
        Assertions.assertNotNull(keys)
        Assertions.assertEquals(entries.second.size, keys.keys.size)
        entries.second.forEach { type -> Assertions.assertTrue(keys.keys.contains(type)) }
        keys.values.forEach { detailList -> Assertions.assertEquals(1, detailList.size) }
    }

    /**
     * Test [MessageQueueController.getAll] to ensure that all entries are returned from the `queueType` when an explicit `queueType` is provided.
     */
    @Test
    fun testGetAll_SpecificQueueType()
    {
        val entries = initialiseMapWithEntries()
        val type = entries.first[0].type
        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ALL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("queueType", type))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
        val keys = gson.fromJson<Map<String, List<String>>>(mvcResult.response.contentAsString, mapType)
        Assertions.assertNotNull(keys)
        Assertions.assertEquals(1, keys.keys.size)
        Assertions.assertTrue(keys.keys.contains(type))
        keys.values.forEach { detailList -> Assertions.assertEquals(1, detailList.size) }
        Assertions.assertEquals(entries.first[0].toDetailedString(false), keys[type]!![0])
    }

    /**
     * Test [MessageQueueController.getOwned] to ensure that no entries are returned when no [QueueMessage] are consumed by the provided `consumedBy` parameter.
     */
    @Test
    fun testGetOwned_NoneOwned()
    {
        val entries = initialiseMapWithEntries()
        val consumedBy = "my-consumed-by-identifier"

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNED)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("consumedBy", consumedBy)
            .param("queueType", entries.first[0].type))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val listType = object : TypeToken<List<MessageResponse>>() {}.type
        val owned = gson.fromJson<List<MessageResponse>>(mvcResult.response.contentAsString, listType)
        Assertions.assertTrue(owned.isEmpty())
    }

    /**
     * Test [MessageQueueController.getOwned] to ensure that all appropriate [QueueMessage] entries are returned when the provided `consumedBy` parameter owns the existings [QueueMessage].
     */
    @Test
    fun testGetOwned_SomeOwned()
    {
        val consumedBy = "my-consumed-by-identifier"
        val type = "my-type"
        val message1 = createQueueMessage(consumedBy = consumedBy, type = type)
        val message2 = createQueueMessage(consumedBy = consumedBy, type = type)

        Assertions.assertTrue(multiQueue.add(message1))
        Assertions.assertTrue(multiQueue.add(message2))

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNED)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("consumedBy", consumedBy)
            .param("queueType", type))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val listType = object : TypeToken<List<MessageResponse>>() {}.type
        val owned = gson.fromJson<List<MessageResponse>>(mvcResult.response.contentAsString, listType)
        Assertions.assertTrue(owned.isNotEmpty())
        owned.forEach { message ->
            Assertions.assertTrue(message.message.uuid == message1.uuid || message.message.uuid == message2.uuid)
            Assertions.assertEquals(type, message.queueType)
            Assertions.assertEquals(type, message.message.type)
            Assertions.assertTrue(message.message.consumed)
            Assertions.assertEquals(consumedBy, message.message.consumedBy)
        }
    }

    /**
     * Test [MessageQueueController.consumeMessage] to ensure that [HttpStatus.NOT_FOUND] is returned when a [QueueMessage] with the provided [UUID] does not exist.
     */
    @Test
    fun testConsumeMessage_doesNotExist()
    {
        val uuid = UUID.randomUUID().toString()
        val consumedBy = "consumer"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_CONSUME)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("consumedBy", consumedBy)
            .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Test [MessageQueueController.consumeMessage] to ensure that the message is consumed correctly and [HttpStatus.OK] is returned when the [QueueMessage] was initially not consumed.
     */
    @Test
    fun testConsumeMessage_messageIsConsumed()
    {
        val consumedBy = "consumer"
        val message = createQueueMessage()
        Assertions.assertFalse(message.consumed)
        Assertions.assertNull(message.consumedBy)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_CONSUME)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("consumedBy", consumedBy)
            .param("uuid", message.uuid.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertTrue(messageResponse.message.consumed)
        Assertions.assertEquals(consumedBy, messageResponse.message.consumedBy)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.consumeMessage] to ensure that the message is consumed correctly and [HttpStatus.ACCEPTED] is returned when the [QueueMessage] is already consumed by the provided `consumedBy` identifier.
     */
    @Test
    fun testConsumeMessage_alreadyConsumedBySameID()
    {
        val consumedBy = "consumer"
        val message = createQueueMessage(consumedBy = consumedBy)

        Assertions.assertTrue(message.consumed)
        Assertions.assertEquals(consumedBy, message.consumedBy)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_CONSUME)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("consumedBy", consumedBy)
            .param("uuid", message.uuid.toString()))
            .andExpect(MockMvcResultMatchers.status().isAccepted)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertTrue(messageResponse.message.consumed)
        Assertions.assertEquals(message.consumedBy, messageResponse.message.consumedBy)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.consumeMessage] to ensure that [HttpStatus.CONFLICT] is returned when the [QueueMessage] is already consumed by another identifier.
     */
    @Test
    fun testConsumeMessage_alreadyConsumedByOtherID()
    {
        val consumedBy = "consumer"
        val message = createQueueMessage(consumedBy = consumedBy)

        Assertions.assertTrue(message.consumed)
        Assertions.assertEquals(consumedBy, message.consumedBy)
        Assertions.assertTrue(multiQueue.add(message))

        val wrongConsumer = "wrong-consumer"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_CONSUME)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("consumedBy", wrongConsumer)
            .param("uuid", message.uuid.toString()))
            .andExpect(MockMvcResultMatchers.status().isConflict)
    }

    /**
     * Test [MessageQueueController.getNext] to ensure [HttpStatus.NO_CONTENT] is returned when there are no [QueueMessage]s available for the provided `queueType`.
     */
    @Test
    fun testGetNext_noNewMessages()
    {
        val consumedBy = "consumer"
        val type = "type"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("queueType", type)
            .param("consumedBy", consumedBy))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Test [MessageQueueController.getNext] to ensure that [HttpStatus.NO_CONTENT] is returned if there are no `unconsumed` [QueueMessage]s available.
     */
    @Test
    fun testGetNext_noNewUnConsumedMessages()
    {
        val consumedBy = "consumer"
        val type = "type"
        val message = createQueueMessage(type = type, consumedBy = consumedBy)
        val message2 = createQueueMessage(type = type, consumedBy = consumedBy)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("queueType", type)
            .param("consumedBy", consumedBy))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Test [MessageQueueController.getNext] to ensure that the correct next message is returned if it exists in the [MultiQueue].
     */
    @Test
    fun testGetNext()
    {
        val consumedBy = "consumer"
        val type = "type"
        val message = createQueueMessage(type = type, consumedBy = consumedBy)
        val message2 = createQueueMessage(type = type)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("queueType", type)
            .param("consumedBy", consumedBy))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertTrue(messageResponse.message.consumed)
        Assertions.assertEquals(consumedBy, messageResponse.message.consumedBy)
        Assertions.assertEquals(message2.uuid, messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that [HttpStatus.NOT_FOUND] is returned when a [QueueMessage] with the provided [UUID] does not exist.
     */
    @Test
    fun testReleaseMessage_doesNotExist()
    {
        val uuid = UUID.randomUUID().toString()
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("uuid", uuid))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that the [QueueMessage] is released if it is currently consumed.
     */
    @Test
    fun testReleaseMessage_messageIsReleased()
    {
        val consumedBy = "consumer"
        val message = createQueueMessage(consumedBy = consumedBy)

        Assertions.assertTrue(message.consumed)
        Assertions.assertEquals(consumedBy, message.consumedBy)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("uuid", message.uuid.toString())
            .param("consumedBy", consumedBy))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertFalse(messageResponse.message.consumed)
        Assertions.assertNull(messageResponse.message.consumedBy)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that the [QueueMessage] is released if it is currently consumed. Even when the `consumedBy` is not provided.
     */
    @Test
    fun testReleaseMessage_messageIsReleased_withoutConsumedByInQuery()
    {
        val consumedBy = "consumer"
        val message = createQueueMessage(consumedBy = consumedBy)

        Assertions.assertTrue(message.consumed)
        Assertions.assertEquals(consumedBy, message.consumedBy)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("uuid", message.uuid.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertFalse(messageResponse.message.consumed)
        Assertions.assertNull(messageResponse.message.consumedBy)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that [HttpStatus.ACCEPTED] is returned if the message is already released and not owned by anyone.
     */
    @Test
    fun testReleaseMessage_alreadyReleased()
    {
        val message = createQueueMessage()
        Assertions.assertFalse(message.consumed)
        Assertions.assertNull(message.consumedBy)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("uuid", message.uuid.toString()))
            .andExpect(MockMvcResultMatchers.status().isAccepted)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertFalse(messageResponse.message.consumed)
        Assertions.assertNull(messageResponse.message.consumedBy)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that [HttpStatus.CONFLICT] is returned if `consumedBy` is provided and does not match the [QueueMessage.consumedBy], meaning the user cannot `release` the [QueueMessage] if it's not the `consumer` of the [QueueMessage].
     */
    @Test
    fun testReleaseMessage_cannotBeReleasedWithMisMatchingID()
    {
        val consumedBy = "consumer"
        val message = createQueueMessage(consumedBy = consumedBy)

        Assertions.assertTrue(message.consumed)
        Assertions.assertEquals(consumedBy, message.consumedBy)
        Assertions.assertTrue(multiQueue.add(message))

        val wrongConsumedBy = "wrong-consumer"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("uuid", message.uuid.toString())
            .param("consumedBy", wrongConsumedBy))
            .andExpect(MockMvcResultMatchers.status().isConflict)
    }

    /**
     * A helper method which creates `4` [QueueMessage] objects and inserts them into the [MultiQueue].
     *
     * @return a [Pair] containing the [List] of [QueueMessage] and their related matching [List] of [String] `queueTypes` in order.
     */
    private fun initialiseMapWithEntries(): Pair<List<QueueMessage>, List<String>>
    {
        val types = listOf("type1", "type2", "type3", "type4")
        val message = createQueueMessage(type = types[0])
        val message2 = createQueueMessage(type = types[1])
        val message3 = createQueueMessage(type = types[2], consumedBy = "consumed")
        val message4 = createQueueMessage(type = types[3])

        multiQueue.add(message)
        multiQueue.add(message2)
        multiQueue.add(message3)
        multiQueue.add(message4)

        return Pair(listOf(message, message2, message3, message4), types)
    }

    /**
     * A helper method to create a [QueueMessage] that can be easily re-used between each test.
     *
     * @param type the `queueType` to assign to the created [QueueMessage]
     * @param consumedBy indicates whether the [QueueMessage.consumedBy] should be non-null and [QueueMessage.consumed] should be `true`.
     * @return a [QueueMessage] initialised with multiple parameters
     */
    private fun createQueueMessage(type: String = "a-type", consumedBy: String? = null): QueueMessage
    {
        val uuid = UUID.randomUUID().toString()
        val payload = Payload("test", 12, true, PayloadEnum.C)
        val message = QueueMessage(payload = payload, type = type)
        message.uuid = UUID.fromString(uuid)

        if (consumedBy != null)
        {
            message.consumed = true
            message.consumedBy = consumedBy
        }
        return message
    }
}
