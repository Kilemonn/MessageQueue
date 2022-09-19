package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.Payload
import au.kilemon.messagequeue.PayloadEnum
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.MessageResponse
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.google.gson.Gson
import org.junit.jupiter.api.Assertions
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
import org.springframework.web.server.ResponseStatusException
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
    private lateinit var queueController: MessageQueueController

    @Autowired
    private lateinit var multiQueue: MultiQueue

    private val gson: Gson = Gson()

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
     * Calling create with provided [QueueMessage.consumedBy], [QueueMessage.consumed] and [QueueMessage.uuid] to
     * ensure they are set correctly in the returned [MessageResponse].
     */
    @Test
    fun testCreateQueueEntry_withProvidedDefaults()
    {
        val payload = Payload("Some test string", 89, true, PayloadEnum.C)
        val isConsumed = true
        val consumedBy = "instance_a"
        val type = "A_TYPE"
        val uuid = UUID.randomUUID()

        val message = QueueMessage(payload, type, consumed = isConsumed, consumedBy = consumedBy)
        message.uuid = uuid

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(payload, deserialisedPayload)
        Assertions.assertEquals(isConsumed, messageResponse.message.consumed)
        Assertions.assertEquals(consumedBy, messageResponse.message.consumedBy)
        Assertions.assertEquals(type, messageResponse.message.type)
        Assertions.assertEquals(type, messageResponse.queueType)
        Assertions.assertEquals(uuid, messageResponse.message.uuid)
    }

    /**
     * Calling create without [QueueMessage.consumedBy], [QueueMessage.consumed] and [QueueMessage.uuid] to
     * ensure they are initialised as expected when they are not provided by the caller.
     */
    @Test
    fun testCreateQueueEntry_withOutDefaults()
    {
        val payload = Payload("Some test string", 23, true, PayloadEnum.C)
        val type = "A_TYPE"
        val message = QueueMessage(payload, type)

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(payload, deserialisedPayload)
        Assertions.assertFalse(messageResponse.message.consumed)
        Assertions.assertNull(messageResponse.message.consumedBy)
        Assertions.assertEquals(type, messageResponse.message.type)
        Assertions.assertEquals(type, messageResponse.queueType)
        Assertions.assertNotNull(messageResponse.message.uuid)
    }

    @Test
    fun testGetEntry()
    {
        val uuid = UUID.randomUUID().toString()
        val type = "a-type"
        val message = QueueMessage(payload = "ok", type = type)
        message.uuid = UUID.fromString(uuid)
        message.consumed = false

        multiQueue.add(message)

        val returnedMessage = queueController.getEntry(uuid)
        Assertions.assertEquals(HttpStatus.OK, returnedMessage.statusCode)
        val res = returnedMessage.body
        Assertions.assertNotNull(res)
        Assertions.assertEquals(type, res!!.queueType)
        Assertions.assertEquals(message, res.message)

        multiQueue.clear()
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
     * Test [MessageQueueController.getEntry] to ensure that a [ResponseStatusException] is thrown with [HttpStatus.NOT_FOUND] when a [UUID] of a message that does not exist is provided.
     */
    @Test
    fun testGetEntry_Exception_NotExists()
    {
        val uuid = "invalid-not-found-uuid"
        val exception = Assertions.assertThrows(ResponseStatusException::class.java)
        {
            queueController.getEntry(uuid)
        }
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }
}
