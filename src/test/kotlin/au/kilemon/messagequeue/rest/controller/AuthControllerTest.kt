package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.token.JwtTokenProvider
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.rest.response.AuthResponse
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.google.gson.Gson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

/**
 * A test class for the [AuthController].
 * Providing tests of the context and endpoint validation/handling itself.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
@WebMvcTest(controllers = [AuthController::class], properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=IN_MEMORY"])
@Import(*[QueueConfiguration::class, LoggingConfiguration::class])
class AuthControllerTest
{
    /**
     * A [TestConfiguration] for the outer class.
     *
     * @author github.com/Kilemonn
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

    // Setting as a Spy to override it to replicate different scenarios
    @SpyBean
    private lateinit var multiQueueAuthenticator: MultiQueueAuthenticator

    // Setting as a Spy to override it to replicate different scenarios
    @SpyBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val gson: Gson = Gson()

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.NO_CONTENT] when the
     * [MultiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.NONE].
     */
    @Test
    fun testRestrictSubQueue_inNoneMode()
    {
        val queueType = "testRestrictSubQueue_inNoneMode"
        Assertions.assertEquals(MultiQueueAuthenticationType.NONE, multiQueueAuthenticator.getAuthenticationType())
        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${queueType}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.CONFLICT] when the
     * requested queue type requested is already restricted.
     */
    @Test
    fun testRestrictSubQueue_alreadyRestricted()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.HYBRID).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.HYBRID, multiQueueAuthenticator.getAuthenticationType())

        val queueType = "testRestrictSubQueue_alreadyRestricted"
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(queueType))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(queueType))

        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${queueType}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isConflict)
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR]
     * when the requested queue type fails to be restricted.
     */
    @Test
    fun testRestrictSubQueue_wasNotAdded()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.HYBRID).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.HYBRID, multiQueueAuthenticator.getAuthenticationType())

        val queueType = "testRestrictSubQueue_wasNotAdded"
        Mockito.doReturn(false).`when`(multiQueueAuthenticator).addRestrictedEntry(queueType)

        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${queueType}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR]
     * when the token fails to generate.
     */
    @Test
    fun testRestrictSubQueue_tokenGenerationFailure()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.HYBRID).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.HYBRID, multiQueueAuthenticator.getAuthenticationType())

        val queueType = "testRestrictSubQueue_tokenGenerationFailure"
        Mockito.doReturn(Optional.empty<String>()).`when`(jwtTokenProvider).createTokenForSubQueue(queueType)

        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${queueType}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.OK] and a valid
     * [AuthResponse] when the requested queue type is restricted successfully.
     */
    @Test
    fun testRestrictSubQueue_tokenGenerated()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.HYBRID).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.HYBRID, multiQueueAuthenticator.getAuthenticationType())

        val queueType = "testRestrictSubQueue_tokenGenerated"

        val mvcResult: MvcResult = mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${queueType}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val authResponse = gson.fromJson(mvcResult.response.contentAsString, AuthResponse::class.java)
        Assertions.assertNotNull(authResponse.token)
        Assertions.assertNotNull(authResponse.correlationId)
        Assertions.assertEquals(queueType, authResponse.subQueue)
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns [org.springframework.http.HttpStatus.NO_CONTENT]
     * when the [MultiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.NONE].
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_inNoneMode()
    {
        val queueType = "testRemoveRestrictionFromSubQueue_inNoneMode"

        Assertions.assertEquals(MultiQueueAuthenticationType.NONE, multiQueueAuthenticator.getAuthenticationType())
        mockMvc.perform(
            MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${queueType}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

}
