package au.kilemon.messagequeue.filter

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.MDC
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 *
 * @author github.com/Kilemonn
 */
class JwtAuthenticationFilterTest
{
    private val jwtAuthenticationFilter = JwtAuthenticationFilter()

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
        val authHeaderValue = "testGetSubQueueInTokenFromHeaders_headerExists"
        Mockito.`when`(request.getHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER)).thenReturn(authHeaderValue)

        val subQueue = jwtAuthenticationFilter.getSubQueueInTokenFromHeaders(request)
        Assertions.assertTrue(subQueue.isPresent)
        Assertions.assertEquals(authHeaderValue, subQueue.get())
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
}
