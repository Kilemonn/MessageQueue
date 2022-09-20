package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.Payload
import au.kilemon.messagequeue.PayloadEnum
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.MessageResponse
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.google.gson.Gson
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
        val message = createQueueMessage(false)

        multiQueue.add(message)

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
        val message = createQueueMessage(true)

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

        multiQueue.add(message)

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
        val types = listOf("type1", "type2", "type3", "type4")
        val message = createQueueMessage(type = types[0])
        val message2 = createQueueMessage(type = types[1])
        val message3 = createQueueMessage(type = types[2])
        val message4 = createQueueMessage(type = types[3])

        multiQueue.add(message)
        multiQueue.add(message2)
        multiQueue.add(message3)
        multiQueue.add(message4)

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val keys = gson.fromJson(mvcResult.response.contentAsString, List::class.java)
        Assertions.assertFalse(keys.isNullOrEmpty())
        Assertions.assertEquals(types.size, keys.size)
        types.forEach { type -> Assertions.assertTrue(keys.contains(type)) }
    }

    /**
     * Test [MessageQueueController.getKeys] to ensure that all keys are returned. Specifically when entries are added and `includeEmpty` is set to `false`.
     */
    @Test
    fun testGetKeys_excludeEmpty()
    {
        val types = listOf("type1", "type2", "type3", "type4")
        val message = createQueueMessage(type = types[0])
        val message2 = createQueueMessage(type = types[1])
        val message3 = createQueueMessage(type = types[2])
        val message4 = createQueueMessage(type = types[3])

        multiQueue.add(message)
        multiQueue.add(message2)
        multiQueue.add(message3)
        multiQueue.add(message4)

        multiQueue.remove(message)
        multiQueue.remove(message2)

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("includeEmpty", "false"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val keys = gson.fromJson(mvcResult.response.contentAsString, List::class.java)
        Assertions.assertFalse(keys.isNullOrEmpty())
        Assertions.assertEquals(2, keys.size)
        types.subList(2, 3).forEach { type -> Assertions.assertTrue(keys.contains(type)) }
    }



    /**
     * A helper method to create a [QueueMessage] that can be easily re-used between each test.
     *
     * @param withConsumedBy indicates whether the [QueueMessage.consumedBy] should be non-null and [QueueMessage.consumed] should be `true`.
     * @return a [QueueMessage] initialised with multiple parameters
     */
    private fun createQueueMessage(withConsumedBy: Boolean = false, type: String = "a-type"): QueueMessage
    {
        val uuid = UUID.randomUUID().toString()
        val payload = Payload("test", 12, true, PayloadEnum.C)
        val message = QueueMessage(payload = payload, type = type)
        message.uuid = UUID.fromString(uuid)
        message.consumed = withConsumedBy

        if (withConsumedBy)
        {
            message.consumedBy = "consumer"
        }
        return message
    }
}
