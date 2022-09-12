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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [SettingsController::class])
class SettingsControllerTest
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
    fun testGetSettings()
    {
        mockMvc.perform(
            MockMvcRequestBuilders.get(SettingsController.SETTINGS_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("{\n    \"multiQueueType\": \"IN_MEMORY\"\n}"))
//            .andExpect(MockMvcResultMatchers.jsonPath("$.muiltiQueueType").value(MultiQueueType.IN_MEMORY.toString()))
    }
}
