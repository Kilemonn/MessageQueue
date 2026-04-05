package au.kilemon.messagequeue.authentication.token

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.exception.NoKeyProvidedException
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

/**
 * A test class for [JwtTokenProvider] and different token issuing and verification scenarios. Specifically the
 * restriction mode is set to [au.kilemon.messagequeue.authentication.RestrictionMode.NONE].
 *
 * @author github.com/Kilemonn
 */
@TestPropertySource(properties = ["${MessageQueueSettings.RESTRICTION_MODE}=${MessageQueueSettings.RESTRICTION_MODE_DEFAULT}"])
class NoneModeJwtTokenProviderTest : JwtTokenProviderTest()
{
    @BeforeEach
    fun setup()
    {
        Assertions.assertEquals(RestrictionMode.NONE, restrictionMode)
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
        Assertions.assertTrue(token.isEmpty)
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
        val token = jwtTokenProvider.createTokenForSubQueue(subQueue, expiryInMinutes)
        Assertions.assertNotNull(token)
        Assertions.assertTrue(token.isEmpty)
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
        Assertions.assertEquals("", jwtTokenProvider.tokenKey)
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
