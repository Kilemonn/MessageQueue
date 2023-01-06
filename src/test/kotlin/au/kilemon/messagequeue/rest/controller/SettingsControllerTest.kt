package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.MultiQueueType
import com.google.gson.Gson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * A test class for the [SettingsController] class. Mainly testing the endpoints in the `RestController`.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [SettingsController::class], properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=IN_MEMORY"])
@Import(LoggingConfiguration::class)
class SettingsControllerTest
{
    /**
     * The test configuration to be used by the [SettingsControllerTest] class.
     *
     * @author github.com/KyleGonzalez
     */
    @TestConfiguration
    internal class TestConfig
    {
        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set the here, set them in the [WebMvcTest.properties].
         */
        @Bean
        fun getSettings(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val gson: Gson = Gson()

    /**
     * Test [SettingsController.getSettings] and verify the response payload and default values.
     */
    @Test
    fun testGetSettings_defaultValues()
    {
        val mvcResult: MvcResult = mockMvc.perform(MockMvcRequestBuilders.get(SettingsController.SETTINGS_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val settings = gson.fromJson(mvcResult.response.contentAsString, MessageQueueSettings::class.java)
        Assertions.assertEquals(MultiQueueType.IN_MEMORY.toString(), settings.multiQueueType)
        Assertions.assertTrue(settings.redisPrefix.isEmpty())
        Assertions.assertEquals(MessageQueueSettings.REDIS_ENDPOINT_DEFAULT, settings.redisEndpoint)
        Assertions.assertEquals("false", settings.redisUseSentinels)
        Assertions.assertEquals(MessageQueueSettings.REDIS_MASTER_NAME_DEFAULT, settings.redisMasterName)
        Assertions.assertTrue(settings.sqlEndpoint.isEmpty())
        Assertions.assertTrue(settings.sqlUsername.isEmpty())
    }
}
