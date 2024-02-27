package au.kilemon.messagequeue.filter

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthenticationException
import au.kilemon.messagequeue.authentication.token.JwtTokenProvider
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.rest.controller.MessageQueueController
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite
import java.util.*

/**
 * A test class for the [JwtAuthenticationFilter].
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JwtAuthenticationFilter::class])
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class JwtAuthenticationFilterTest
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
        open fun getHandlerExceptionResolver(): HandlerExceptionResolver
        {
            return HandlerExceptionResolverComposite()
        }
    }

    @Autowired
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider



    @BeforeEach
    fun setUp()
    {
        MDC.clear()
    }

    /**
     * This should be afterclass but works better here.
     */
    @AfterEach
    fun tearDown()
    {
        MDC.clear()
    }

    /**
     * Ensure that [JwtAuthenticationFilter.setSubQueue] does set the [MDC] [JwtAuthenticationFilter.SUB_QUEUE] property
     * if the provided [Optional] is [Optional.isPresent].
     */
    @Test
    fun testSetSubQueue_valuePresent()
    {
        val subQueue = Optional.of("testSetSubQueue_valuePresent")
        Assertions.assertTrue(subQueue.isPresent)
        Assertions.assertNull(MDC.get(JwtAuthenticationFilter.SUB_QUEUE))

        jwtAuthenticationFilter.setSubQueue(subQueue)

        Assertions.assertEquals(subQueue.get(), MDC.get(JwtAuthenticationFilter.SUB_QUEUE))
    }

    /**
     * Ensure that [JwtAuthenticationFilter.setSubQueue] does not set the [MDC] [JwtAuthenticationFilter.SUB_QUEUE]
     * property if the provided [Optional] is [Optional.isEmpty].
     */
    @Test
    fun testSetSubQueue_valueEmpty()
    {
        val subQueue = Optional.empty<String>()
        Assertions.assertTrue(subQueue.isEmpty)
        Assertions.assertNull(MDC.get(JwtAuthenticationFilter.SUB_QUEUE))

        jwtAuthenticationFilter.setSubQueue(subQueue)

        Assertions.assertNull(MDC.get(JwtAuthenticationFilter.SUB_QUEUE))
    }

    /**
     * Ensure that [JwtAuthenticationFilter.getSubQueueInTokenFromHeaders] will retrieve the value correctly from the
     * [JwtAuthenticationFilter.AUTHORIZATION_HEADER] and return it.
     */
    @Test
    fun testGetSubQueueInTokenFromHeaders_headerExists()
    {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val initialSubQueue = "testGetSubQueueInTokenFromHeaders_headerExists"
        val token = jwtTokenProvider.createTokenForSubQueue(initialSubQueue)
        Assertions.assertTrue(token.isPresent)
        val authHeaderValue = "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}"
        Mockito.`when`(request.getHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER)).thenReturn(authHeaderValue)

        val subQueue = jwtAuthenticationFilter.getSubQueueInTokenFromHeaders(request)
        Assertions.assertTrue(subQueue.isPresent)
        Assertions.assertEquals(initialSubQueue, subQueue.get())
    }

    /**
     * Ensure that [JwtAuthenticationFilter.getSubQueueInTokenFromHeaders] will fail to retrieve and verify the token
     * when it does not have the [JwtAuthenticationFilter.BEARER_HEADER_VALUE] prefix.
     */
    @Test
    fun testGetSubQueueInTokenFromHeaders_withoutBearerPrefix()
    {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val authHeaderValue = "testGetSubQueueInTokenFromHeaders_withoutBearerPrefix"
        Mockito.`when`(request.getHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER)).thenReturn(authHeaderValue)

        val subQueue = jwtAuthenticationFilter.getSubQueueInTokenFromHeaders(request)
        Assertions.assertTrue(subQueue.isEmpty)
    }

    /**
     * Ensure that [JwtAuthenticationFilter.getSubQueueInTokenFromHeaders] will return an [Optional.empty] when the
     * [JwtAuthenticationFilter.AUTHORIZATION_HEADER] header value is `null`.
     */
    @Test
    fun testGetSubQueueInTokenFromHeaders_headerDoesNotExists()
    {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER)).thenReturn(null)

        val subQueue = jwtAuthenticationFilter.getSubQueueInTokenFromHeaders(request)
        Assertions.assertTrue(subQueue.isEmpty)
    }

    /**
     * Ensure the [JwtAuthenticationFilter.getSubQueue] retrieves the stored [JwtAuthenticationFilter.SUB_QUEUE]
     * property from the [MDC].
     */
    @Test
    fun testGetSubQueue()
    {
        val subQueue = "testGetSubQueue"
        Assertions.assertNull(JwtAuthenticationFilter.getSubQueue())

        jwtAuthenticationFilter.setSubQueue(Optional.of(subQueue))
        Assertions.assertEquals(subQueue, JwtAuthenticationFilter.getSubQueue())
    }

    /**
     * Ensure that [JwtAuthenticationFilter.tokenIsPresentAndQueueIsRestricted] returns `true` when the provided
     * token is present and the queue is restricted.
     */
    @Test
    fun testTokenIsPresentAndQueueIsRestricted_TokenPresentAndQueueRestricted()
    {
        val subQueue = Optional.of("testTokenIsPresentAndQueueIsRestricted_TokenPresentAndQueueRestricted")
        val authenticator = Mockito.mock(MultiQueueAuthenticator::class.java)
        Mockito.`when`(authenticator.isRestricted(subQueue.get())).thenReturn(true)

        Assertions.assertTrue(jwtAuthenticationFilter.tokenIsPresentAndQueueIsRestricted(subQueue, authenticator))
    }

    /**
     * Ensure that [JwtAuthenticationFilter.tokenIsPresentAndQueueIsRestricted] returns `false` when the provided
     * token is present and the queue is NOT restricted.
     */
    @Test
    fun testTokenIsPresentAndQueueIsRestricted_TokenPresentAndQueueNotRestricted()
    {
        val subQueue = Optional.of("testTokenIsPresentAndQueueIsRestricted_TokenPresentAndQueueNotRestricted")
        val authenticator = Mockito.mock(MultiQueueAuthenticator::class.java)
        Mockito.`when`(authenticator.isRestricted(subQueue.get())).thenReturn(false)

        Assertions.assertFalse(jwtAuthenticationFilter.tokenIsPresentAndQueueIsRestricted(subQueue, authenticator))
    }

    /**
     * Ensure that [JwtAuthenticationFilter.tokenIsPresentAndQueueIsRestricted] returns `false` when the provided
     * token is empty.
     */
    @Test
    fun testTokenIsPresentAndQueueIsRestricted_TokenNotPresent()
    {
        val subQueue = Optional.empty<String>()
        val authenticator = Mockito.mock(MultiQueueAuthenticator::class.java)

        Assertions.assertFalse(jwtAuthenticationFilter.tokenIsPresentAndQueueIsRestricted(subQueue, authenticator))
    }

    /**
     * Ensure [JwtAuthenticationFilter.canSkipTokenVerification] returns `true` when the provided URI does not
     * match the "no auth required" list.
     */
    @Test
    fun testUrlRequiresAuthentication_notInWhitelist()
    {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val uriPath = "/another/test/endpoint${MessageQueueController.MESSAGE_QUEUE_BASE_PATH}"
        Mockito.`when`(request.requestURI).thenReturn(uriPath)
        Mockito.`when`(request.method).thenReturn(HttpMethod.POST.toString())

        Assertions.assertFalse(jwtAuthenticationFilter.canSkipTokenVerification(request))
    }

    /**
     * Ensure [JwtAuthenticationFilter.canSkipTokenVerification] returns `false` when the provided URI does
     * start with an un-authorised path prefix.
     */
    @Test
    fun testUrlRequiresAuthentication_authRequiredURL()
    {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val uriPath = "${MessageQueueController.MESSAGE_QUEUE_BASE_PATH}${MessageQueueController.ENDPOINT_HEALTH_CHECK}"
        Mockito.`when`(request.requestURI).thenReturn(uriPath)
        Mockito.`when`(request.method).thenReturn(HttpMethod.GET.toString())

        Assertions.assertTrue(jwtAuthenticationFilter.canSkipTokenVerification(request))
    }

    /**
     * Ensure [JwtAuthenticationFilter.canSkipTokenVerification] returns `true` when the provided HTTP method is
     * not in the whitelist.
     */
    @Test
    fun testUrlRequiresAuthentication_nonMatchingMethod()
    {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val uriPath = "/a/path"
        Mockito.`when`(request.requestURI).thenReturn(uriPath)
        Mockito.`when`(request.method).thenReturn(HttpMethod.OPTIONS.toString())

        Assertions.assertFalse(jwtAuthenticationFilter.canSkipTokenVerification(request))
    }

    /**
     * Ensure that [JwtAuthenticationFilter.isValidJwtToken] returns a valid [Optional] with the correct sub-queue
     * value when a valid token is provided.
     */
    @Test
    fun testIsValidJwtToken_validToken()
    {
        val subQueue = "testIsValidJwtToken_validToken"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        val embeddedSubQueue = jwtAuthenticationFilter.isValidJwtToken(token.get())
        Assertions.assertTrue(embeddedSubQueue.isPresent)
        Assertions.assertEquals(subQueue, embeddedSubQueue.get())
    }

    /**
     * Ensure that [JwtAuthenticationFilter.isValidJwtToken] throws a [MultiQueueAuthenticationException] when the
     * provided token is invalid.
     */
    @Test
    fun testIsValidJwtToken_throws()
    {
        val token = "testIsValidJwtToken_throws"
        Assertions.assertThrows(MultiQueueAuthenticationException::class.java) {
            jwtAuthenticationFilter.isValidJwtToken(token)
        }
    }
}
