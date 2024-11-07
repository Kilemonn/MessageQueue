package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.token.JwtTokenProvider
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.filter.JwtAuthenticationFilter
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.AuthResponse
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
@WebMvcTest(controllers = [AuthController::class], properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=IN_MEMORY"])
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
    private lateinit var multiQueue: MultiQueue

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val gson: Gson = Gson()

    @BeforeEach
    fun setUp()
    {
        multiQueueAuthenticator.clearRestrictedSubQueues()
        multiQueue.clear()
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.NO_CONTENT] when the
     * [RestrictionMode] is set to [RestrictionMode.NONE].
     */
    @Test
    fun testRestrictSubQueue_inNoneMode()
    {
        val subQueue = "testRestrictSubQueue_inNoneMode"
        Assertions.assertEquals(RestrictionMode.NONE, multiQueueAuthenticator.getRestrictionMode())
        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${subQueue}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.CONFLICT] when the
     * requested sub-queue requested is already restricted.
     */
    @Test
    fun testRestrictSubQueue_alreadyRestricted()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRestrictSubQueue_alreadyRestricted"
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${subQueue}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isConflict)
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR]
     * when the requested sub-queue fails to be restricted.
     */
    @Test
    fun testRestrictSubQueue_wasNotAdded()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRestrictSubQueue_wasNotAdded"
        Mockito.doReturn(false).`when`(multiQueueAuthenticator).addRestrictedEntry(subQueue)

        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${subQueue}")
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
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRestrictSubQueue_tokenGenerationFailure"
        Mockito.doReturn(Optional.empty<String>()).`when`(jwtTokenProvider).createTokenForSubQueue(subQueue)

        mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${subQueue}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    /**
     * Ensure [AuthController.restrictSubQueue] returns [org.springframework.http.HttpStatus.OK] and a valid
     * [AuthResponse] when the requested sub-queue is restricted successfully.
     */
    @Test
    fun testRestrictSubQueue_tokenGenerated()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRestrictSubQueue_tokenGenerated"

        val mvcResult: MvcResult = mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${subQueue}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val authResponse = gson.fromJson(mvcResult.response.contentAsString, AuthResponse::class.java)
        Assertions.assertNotNull(authResponse.token)
        Assertions.assertEquals(subQueue, authResponse.subQueue)
    }

    /**
     * Ensure that even in [RestrictionMode.RESTRICTED] mode we can call the
     * [AuthController.restrictSubQueue] this is important, without this being accessible new sub-queues can never
     * be restricted meaning the queue would be completely inaccessible.
     */
    @Test
    fun testRestrictSubQueue_inRestrictedMode()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRestrictSubQueue_inRestrictedMode"

        val mvcResult: MvcResult = mockMvc.perform(
            MockMvcRequestBuilders.post("${AuthController.AUTH_PATH}/${subQueue}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val authResponse = gson.fromJson(mvcResult.response.contentAsString, AuthResponse::class.java)
        Assertions.assertNotNull(authResponse.token)
        Assertions.assertEquals(subQueue, authResponse.subQueue)
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns [org.springframework.http.HttpStatus.NO_CONTENT]
     * when the [RestrictionMode] is set to [RestrictionMode.NONE].
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_inNoneMode()
    {
        val subQueue = "testRemoveRestrictionFromSubQueue_inNoneMode"

        Assertions.assertEquals(RestrictionMode.NONE, multiQueueAuthenticator.getRestrictionMode())
        mockMvc.perform(
            MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${subQueue}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isAccepted)
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns [org.springframework.http.HttpStatus.FORBIDDEN]
     * when the provided token does not
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_invalidToken()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRemoveRestrictionFromSubQueue_invalidToken"
        val invalidsubQueue = "invalidsubQueue"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        mockMvc.perform(
            MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${invalidsubQueue}")
                .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns [org.springframework.http.HttpStatus.UNAUTHORIZED]
     * when there is no auth token provided and the [RestrictionMode] is
     * [RestrictionMode.RESTRICTED].
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_withoutAuthToken()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRemoveRestrictionFromSubQueue_withoutAuthToken"

        mockMvc.perform(
            MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${subQueue}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns [org.springframework.http.HttpStatus.NO_CONTENT]
     * when the token is valid but there is no restriction placed on the requested sub-queue.
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_validTokenButNotRestricted()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRemoveRestrictionFromSubQueue_validToken"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        mockMvc.perform(
            MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${subQueue}")
                .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns
     * [org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR] when there is an error when attempting to remove
     * the restriction on the sub-queue.
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_failedToRemoveRestriction()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRemoveRestrictionFromSubQueue_failedToRemoveRestriction"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        Mockito.doReturn(false).`when`(multiQueueAuthenticator).removeRestriction(subQueue)

        mockMvc.perform(
            MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${subQueue}")
                .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns [org.springframework.http.HttpStatus.OK] when
     * the sub-queue restriction is removed BUT the sub-queue is not cleared when the query parameter
     * [RestParameters.CLEAR_QUEUE] is not provided.
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_removeButDontClearQueue()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRemoveRestrictionFromSubQueue_removeButDontClearQueue"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        multiQueue.clear()
        try
        {
            Assertions.assertTrue(multiQueue.add(QueueMessage("a payload", subQueue)))
            Assertions.assertEquals(1, multiQueue.size)

            mockMvc.perform(
                MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${subQueue}")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)

            Assertions.assertEquals(1, multiQueue.size)
            Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        }
        finally
        {
            multiQueue.clear()
        }
    }

    /**
     * Ensure [AuthController.removeRestrictionFromSubQueue] returns [org.springframework.http.HttpStatus.OK] when
     * the sub-queue restriction is removed AND make sure the sub-queue is cleared when the query parameter
     * [RestParameters.CLEAR_QUEUE] is provided and set to `true`.
     */
    @Test
    fun testRemoveRestrictionFromSubQueue_removeAndClearQueue()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testRemoveRestrictionFromSubQueue_removeAndClearQueue"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        multiQueue.clear()
        try
        {
            Assertions.assertTrue(multiQueue.add(QueueMessage("a payload", subQueue)))
            Assertions.assertEquals(1, multiQueue.size)

            mockMvc.perform(
                MockMvcRequestBuilders.delete("${AuthController.AUTH_PATH}/${subQueue}?${RestParameters.CLEAR_QUEUE}=true")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)

            Assertions.assertEquals(0, multiQueue.size)
            Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        }
        finally
        {
            multiQueue.clear()
        }
    }

    /**
     * Ensure [AuthController.getRestrictedSubQueueIdentifiers] returns [org.springframework.http.HttpStatus.NO_CONTENT]
     * when the [RestrictionMode] is [RestrictionMode.NONE].
     */
    @Test
    fun testGetRestrictedSubQueueIdentifiers_inNoneMode()
    {
        Mockito.doReturn(RestrictionMode.NONE).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.NONE, multiQueueAuthenticator.getRestrictionMode())

        mockMvc.perform(MockMvcRequestBuilders.get(AuthController.AUTH_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Ensure [AuthController.getRestrictedSubQueueIdentifiers] returns [org.springframework.http.HttpStatus.OK]
     * when the [RestrictionMode] is not in [RestrictionMode.NONE].
     * And the response set matches the entries that are restricted.
     */
    @Test
    fun testGetRestrictedSubQueueIdentifiers_notInNoneMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val restrictedIdentifiers = setOf("testGetRestrictedSubQueueIdentifiers_inNoneMode1", "testGetRestrictedSubQueueIdentifiers_inNoneMode2",
            "testGetRestrictedSubQueueIdentifiers_inNoneMode3", "testGetRestrictedSubQueueIdentifiers_inNoneMode4", "testGetRestrictedSubQueueIdentifiers_inNoneMode5")

        restrictedIdentifiers.forEach { identifier -> Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(identifier)) }
        restrictedIdentifiers.forEach { identifier -> Assertions.assertTrue(multiQueueAuthenticator.isRestricted(identifier)) }

        val mvcResult: MvcResult = mockMvc.perform(MockMvcRequestBuilders.get(AuthController.AUTH_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val stringSetType = object : TypeToken<Set<String>>() {}.type
        val identifiers = gson.fromJson<Set<String>>(mvcResult.response.contentAsString, stringSetType)

        Assertions.assertEquals(restrictedIdentifiers.size, identifiers.size)
        identifiers.forEach { identifier -> Assertions.assertTrue(restrictedIdentifiers.contains(identifier)) }
    }
}
