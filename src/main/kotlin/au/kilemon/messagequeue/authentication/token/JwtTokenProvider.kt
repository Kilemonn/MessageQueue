package au.kilemon.messagequeue.authentication.token

import au.kilemon.messagequeue.logging.HasLogger
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import org.slf4j.Logger
import java.security.SecureRandom
import java.util.*

/**
 * A class to handle Jwt token creation and verification.
 */
class JwtTokenProvider: HasLogger
{
    companion object
    {
        const val ISSUER = "kilemon/message-queue"

        const val SUB_QUEUE_CLAIM = "Sub-Queue-Identifier"
    }

    override val LOG: Logger = this.initialiseLogger()

    private var jwtVerifier: JWTVerifier? = null

    private var algorithm: Algorithm? = null

    /**
     * Lazily initialise and get the [Algorithm] using [Algorithm.HMAC512] and a random key.
     *
     * @return the shared [Algorithm] instance
     */
    private fun getAlgorithm(): Algorithm
    {
        if (algorithm == null)
        {
            val random = SecureRandom()
            val bytes = ByteArray(128)
            random.nextBytes(bytes)
            algorithm = Algorithm.HMAC512(bytes)
        }
        return algorithm!!
    }

    /**
     * Lazily initialise and get the [JWTVerifier].
     *
     * @return the shared [JWTVerifier] instance
     */
    private fun getVerifier(): JWTVerifier
    {
        if (jwtVerifier == null)
        {
            jwtVerifier = JWT.require(getAlgorithm()).withIssuer(ISSUER).build()
        }
        return jwtVerifier!!
    }

    /**
     * Verify the provided [String] token and decode it and return it as a [DecodedJWT].
     *
     * @param token the token to parse and decode
     * @return the [DecodedJWT] from the provided [String] token, otherwise [Optional.empty]
     */
    fun verifyTokenForSubQueue(token: String): Optional<DecodedJWT>
    {
        try
        {
            return Optional.ofNullable(getVerifier().verify(token))
        }
        catch (ex: JWTVerificationException)
        {
            // Invalid signature/claims
            LOG.error("Failed to verify provided token.", ex)
        }
        return Optional.empty()
    }

    /**
     * Create a new token.
     *
     * @param subQueue will be embedded in the generated token as a claim with key [SUB_QUEUE_CLAIM]
     * @param expiryInMinutes the generated token expiry in minutes, if `null` this is not added the token will be valid
     * indefinitely
     * @return the generated token as a [String] otherwise [Optional.empty] if there was a problem generating the token
     */
    fun createTokenForSubQueue(subQueue: String, expiryInMinutes: Long? = null): Optional<String>
    {
        try
        {
            val builder = JWT.create().withIssuer(ISSUER).withClaim(SUB_QUEUE_CLAIM, subQueue)
            if (expiryInMinutes != null)
            {
                builder.withExpiresAt(Date(Date().time + (expiryInMinutes * 60 * 1000)))
            }
            val token = builder.sign(getAlgorithm())

            LOG.info("Creating new token for sub-queue [{}] with expiry of [{}] minutes.", subQueue, expiryInMinutes)
            return Optional.ofNullable(token)
        }
        catch (ex: JWTCreationException)
        {
            // Invalid Signing configuration / Couldn't convert Claims.
            LOG.error(String.format("Failed to create requested token for sub-queue identifier [%s].", subQueue), ex)
        }
        return Optional.empty()
    }
}
