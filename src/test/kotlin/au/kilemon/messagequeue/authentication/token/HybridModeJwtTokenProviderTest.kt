package au.kilemon.messagequeue.authentication.token

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.exception.NoKeyProvidedException
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import java.util.Date


/**
 * A test class for [JwtTokenProvider] and different token issuing and verification scenarios. Specifically the
 * restriction mode is set to [au.kilemon.messagequeue.authentication.RestrictionMode.HYBRID].
 *
 * @author github.com/Kilemonn
 */
@TestPropertySource(properties = ["${MessageQueueSettings.RESTRICTION_MODE}=HYBRID",
"${MessageQueueSettings.ACCESS_TOKEN_KEY}=${HybridModeJwtTokenProviderTest.KEY}"
])
class HybridModeJwtTokenProviderTest : JwtTokenProviderTest()
{
    companion object
    {
        const val KEY: String = "1234567890123456"
    }

    @BeforeEach
    fun setup()
    {
        Assertions.assertEquals(RestrictionMode.HYBRID, restrictionMode)
    }

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

    /**
     * Ensure the [JwtTokenProvider.tokenKey] is set from the provided property value.
     */
    @Test
    fun testCheckTokenDefaultValue()
    {
        Assertions.assertEquals(KEY, jwtTokenProvider.tokenKey)
    }

    /**
     * Ensure when [JwtTokenProvider.getKey] is called with an empty or blank string that it throws [NoKeyProvidedException].
     */
    @Test
    fun testGetKey_emptyOrBlankKey()
    {
        Assertions.assertThrows(NoKeyProvidedException::class.java) {
            jwtTokenProvider.getKey("")
        }

        Assertions.assertThrows(NoKeyProvidedException::class.java) {
            jwtTokenProvider.getKey("   ")
        }
    }

    /**
     * Ensure when [JwtTokenProvider.getKey] with a non-blank argument that it will be returned as a [ByteArray].
     */
    @Test
    fun testGetKey_withProvidedKey()
    {
        val newKey = "testGetOrGenerateKey_withProvidedKey"
        Assertions.assertTrue(newKey.toByteArray().contentEquals(jwtTokenProvider.getKey(newKey)))
    }
}
