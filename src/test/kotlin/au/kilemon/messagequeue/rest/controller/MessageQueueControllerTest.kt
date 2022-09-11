package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [MessageQueueController::class])
class MessageQueueControllerTest
{
    /**
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

    @Test
    fun testGetQueueEntry()
    {
        val queueType = "test"
        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + queueType)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("0"))
    }
}
