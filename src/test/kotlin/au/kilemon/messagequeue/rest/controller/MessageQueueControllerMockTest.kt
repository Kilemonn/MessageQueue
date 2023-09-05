package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.inmemory.InMemoryMultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * A test class for the [MessageQueueController].
 * Specifically mocking scenarios that are hard to replicate.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [MessageQueueController::class], properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=IN_MEMORY"])
@Import(*[LoggingConfiguration::class])
class MessageQueueControllerMockTest
{
    /**
     * A [TestConfiguration] for the outer [MessageQueueControllerTest] class.
     *
     * @author github.com/Kilemonn
     */
    @TestConfiguration
    open class TestConfig
    {
        @Bean
        open fun getMultiQueue(): MultiQueue
        {
            return Mockito.spy(InMemoryMultiQueue::class.java)
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var multiQueue: MultiQueue

    private val gson: Gson = Gson()

    /**
     * Perform a health check call on the [MessageQueueController] to ensure a [HttpStatus.INTERNAL_SERVER_ERROR] is returned when the health check fails.
     */
    @Test
    fun testGetPerformHealthCheck_failureResponse()
    {
        Mockito.`when`(multiQueue.performHealthCheckInternal()).thenThrow(RuntimeException("Failed to perform health check."))

        mockMvc.perform(
            MockMvcRequestBuilders.get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_HEALTH_CHECK)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andReturn()
    }

    /**
     * Test [MessageQueueController.createMessage] to ensure that an internal server error is returned when [MultiQueue.add] returns `false`.
     */
    @Test
    fun testCreateMessage_addFails()
    {
        val message = QueueMessage("payload", "type")

        Mockito.`when`(multiQueue.add(message)).thenReturn(false)

        mockMvc.perform(
            MockMvcRequestBuilders.post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }
}
