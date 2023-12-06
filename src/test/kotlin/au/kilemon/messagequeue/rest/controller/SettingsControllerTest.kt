package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.StorageMedium
import com.google.gson.Gson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * A test class for the [SettingsController] class. Mainly testing the endpoints in the `RestController`.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [SettingsController::class], properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=IN_MEMORY"])
@Import(*[QueueConfiguration::class, LoggingConfiguration::class])
class SettingsControllerTest
{
    /**
     * The test configuration to be used by the [SettingsControllerTest] class.
     *
     * @author github.com/Kilemonn
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

    @SpyBean
    private lateinit var multiQueueAuthenticator: MultiQueueAuthenticator

    private val gson: Gson = Gson()

    /**
     * A helper method to call [SettingsController.getSettings] and verify the response default values.
     */
    private fun testGetSettings_defaultValues(authenticationType: RestrictionMode)
    {
        Assertions.assertEquals(authenticationType, multiQueueAuthenticator.getRestrictionMode())

        val mvcResult: MvcResult = mockMvc.perform(MockMvcRequestBuilders.get(SettingsController.SETTINGS_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val settings = gson.fromJson(mvcResult.response.contentAsString, MessageQueueSettings::class.java)

        Assertions.assertEquals(StorageMedium.IN_MEMORY.toString(), settings.storageMedium)
        Assertions.assertEquals(RestrictionMode.NONE.toString(), settings.restrictionMode)

        Assertions.assertTrue(settings.redisPrefix.isEmpty())
        Assertions.assertEquals(MessageQueueSettings.REDIS_ENDPOINT_DEFAULT, settings.redisEndpoint)
        Assertions.assertEquals("false", settings.redisUseSentinels)
        Assertions.assertEquals(MessageQueueSettings.REDIS_MASTER_NAME_DEFAULT, settings.redisMasterName)

        Assertions.assertTrue(settings.sqlEndpoint.isEmpty())
        Assertions.assertTrue(settings.sqlUsername.isEmpty())

        Assertions.assertTrue(settings.mongoHost.isEmpty())
        Assertions.assertTrue(settings.mongoPort.isEmpty())
        Assertions.assertTrue(settings.mongoDatabase.isEmpty())
        Assertions.assertTrue(settings.mongoUsername.isEmpty())
        Assertions.assertTrue(settings.mongoUri.isEmpty())
    }

    /**
     * Ensure calls to [SettingsController.getSettings] is still available even then the [RestrictionMode]
     * is set to [RestrictionMode.NONE].
     */
    @Test
    fun testGetSettings_noneMode()
    {
        Mockito.doReturn(RestrictionMode.NONE).`when`(multiQueueAuthenticator).getRestrictionMode()
        testGetSettings_defaultValues(RestrictionMode.NONE)
    }

    /**
     * Ensure calls to [SettingsController.getSettings] is still available even then the [RestrictionMode]
     * is set to [RestrictionMode.HYBRID].
     */
    @Test
    fun testGetSettings_hybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        testGetSettings_defaultValues(RestrictionMode.HYBRID)
    }

    /**
     * Ensure calls to [SettingsController.getSettings] is still available even then the [RestrictionMode]
     * is set to [RestrictionMode.RESTRICTED].
     */
    @Test
    fun testGetSettings_restrictedMode()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        testGetSettings_defaultValues(RestrictionMode.RESTRICTED)
    }
}
