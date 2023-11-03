package au.kilemon.messagequeue.authentication.token

import com.auth0.jwt.exceptions.JWTCreationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

/**
 * A test class for [JwtTokenProvider] and different token issuing and verification scenarios.
 */
@ExtendWith(SpringExtension::class)
class JwtTokenProviderTest
{
    private val jwtTokenProvider = JwtTokenProvider()

    /**
     * Ensure [JwtTokenProvider.createTokenForSubQueue] can successfully provision a token for a sub-queue.
     */
    @Test
    fun testCreateTokenForSubQueue()
    {
        val subQueue = "testCreateTokenForSubQueue"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)

        Assertions.assertNotNull(token)
        Assertions.assertTrue(token.isPresent)
    }

    /**
     * Ensure [JwtTokenProvider.createTokenForSubQueue] returns an [Optional.empty] when the underlying method
     * fails to generate the token and throws a [JWTCreationException].
     */
    @Test
    fun testCreateTokenForSubQueue_failsToCreateToken()
    {
        val mockJwtTokenProvider = Mockito.spy(JwtTokenProvider::class.java)
        val subQueue = "testCreateTokenForSubQueue_failsToCreateToken"

        Mockito.doThrow(JWTCreationException("message", Exception())).`when`(mockJwtTokenProvider).createTokenInternal(subQueue)
        val token = mockJwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isEmpty)
    }

    /**
     * Ensure [JwtTokenProvider.verifyTokenForSubQueue] correctly parses out the token's properties
     * and that the issuer and claim are set correctly. And expiry is not set.
     */
    @Test
    fun testVerifyTokenForSubQueue_withoutExpiry()
    {
        val subQueue = "testVerifyTokenForSubQueue"
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertNotNull(token)
        Assertions.assertTrue(token.isPresent)

        val decodedJwt = jwtTokenProvider.verifyTokenForSubQueue(token.get())
        Assertions.assertNotNull(decodedJwt)
        Assertions.assertTrue(decodedJwt.isPresent)

        val jwt = decodedJwt.get()
        Assertions.assertEquals(subQueue, jwt.getClaim(JwtTokenProvider.SUB_QUEUE_CLAIM).asString())
        Assertions.assertEquals(JwtTokenProvider.ISSUER, jwt.issuer)
        Assertions.assertNull(jwt.expiresAt)
    }

    /**
     * Ensure [JwtTokenProvider.verifyTokenForSubQueue] correctly parses out the token's properties
     * and that the issuer, claim and expiry are set correctly.
     */
    @Test
    fun testVerifyTokenForSubQueue_withExpiringToken()
    {
        val subQueue = "testVerifyTokenForSubQueue_withExpiringToken"
        val expiryInMinutes = 60L
        val date = Date()
        Thread.sleep(1000) // Forcing a sleep here to make sure the token is generated AFTER the current time
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue, expiryInMinutes)
        Assertions.assertNotNull(token)
        Assertions.assertTrue(token.isPresent)

        val decodedJwt = jwtTokenProvider.verifyTokenForSubQueue(token.get())
        Assertions.assertNotNull(decodedJwt)
        Assertions.assertTrue(decodedJwt.isPresent)

        val jwt = decodedJwt.get()
        Assertions.assertEquals(subQueue, jwt.getClaim(JwtTokenProvider.SUB_QUEUE_CLAIM).asString())
        Assertions.assertEquals(JwtTokenProvider.ISSUER, jwt.issuer)
        Assertions.assertNotNull(jwt.expiresAt)
        Assertions.assertTrue(jwt.expiresAt >= Date(date.time + (expiryInMinutes * 60 * 1000)))
        Assertions.assertTrue(jwt.expiresAt < Date(date.time + ((expiryInMinutes + 1) * 60 * 1000)))
    }

    /**
     * Ensure [JwtTokenProvider.verifyTokenForSubQueue] fails to decode a token that has an expiry date that is in the
     * past (expired token).
     */
    @Test
    fun testVerifyTokenForSubQueue_withExpiredToken()
    {
        val subQueue = "testVerifyTokenForSubQueue_withExpiringToken"
        val expiryInMinutes = -10L
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue, expiryInMinutes)

        val decodedJwt = jwtTokenProvider.verifyTokenForSubQueue(token.get())
        Assertions.assertNotNull(decodedJwt)
        Assertions.assertTrue(decodedJwt.isEmpty)
    }

    /**
     * Ensure [JwtTokenProvider.verifyTokenForSubQueue] fails to parse the provided token if it's not a valid JWT token.
     */
    @Test
    fun testVerifyTokenForSubQueue_invalidToken()
    {
        val token = "testVerifyTokenForSubQueue_invalidToken"
        val decodedJwt = jwtTokenProvider.verifyTokenForSubQueue(token)
        Assertions.assertNotNull(decodedJwt)
        Assertions.assertTrue(decodedJwt.isEmpty)
    }
}
