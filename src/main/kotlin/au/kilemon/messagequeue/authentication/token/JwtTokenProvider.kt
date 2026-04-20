package au.kilemon.messagequeue.authentication.token

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.exception.NoKeyProvidedException
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.security.SecureRandom
import java.util.Date
import java.util.Optional


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

    @Autowired
    private lateinit var restrictionMode: RestrictionMode

    private var jwtVerifier: JWTVerifier? = null
    private var algorithm: Algorithm? = null

    @Value("\${${MessageQueueSettings.ACCESS_TOKEN_KEY}:}")
    internal lateinit var tokenKey: String

    @PostConstruct
    fun init()
    {
        if (restrictionMode != RestrictionMode.NONE)
        {
            algorithm = Algorithm.HMAC512(getKey(tokenKey))
            jwtVerifier = JWT.require(algorithm).withIssuer(ISSUER).build()
        }
    }

    /**
     * Get the key to be used for Jwt token generation and verification.
     *
     * @return If a value is provided via [MessageQueueSettings.ACCESS_TOKEN_KEY] then we will use it if it is
     * not blank. Otherwise, a randomly generated a byte array is returned
     */
    fun getKey(key: String): ByteArray
    {
        return if (key.isNotBlank())
        {
            LOG.info("Using provided key in property [{}] as the HMAC512 token generation and verification key.", MessageQueueSettings.ACCESS_TOKEN_KEY)
            key.toByteArray()
        }
        else
        {
            LOG.error("No HMAC512 key provided in property [{}] for token generation and verification key. Stopping message queue.", MessageQueueSettings.ACCESS_TOKEN_KEY)
            throw NoKeyProvidedException(restrictionMode)
        }
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
            return Optional.ofNullable(jwtVerifier?.verify(token))
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
        // The caller should return early if we are in none mode [AuthController].
        if (restrictionMode == RestrictionMode.NONE || algorithm == null)
        {
            LOG.error("Unable to generate token for subqueue [{}] because restriction mode is set to [{}]", subQueue, restrictionMode)
            return Optional.empty()
        }

        try
        {
            val token = createTokenInternal(subQueue, expiryInMinutes)

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

    /**
     * The internal method for creating the token.
     * This is needed for tests instead of using PowerMock or something.
     *
     * @param subQueue will be embedded in the generated token as a claim with key [SUB_QUEUE_CLAIM]
     * @param expiryInMinutes the generated token expiry in minutes, if `null` this is not added the token will be valid
     * indefinitely
     * @return the generated token as a [String]
     * @throws JWTCreationException if there is a problem creating the token
     */
    fun createTokenInternal(subQueue: String, expiryInMinutes: Long? = null): String
    {
        val builder = JWT.create().withIssuer(ISSUER).withClaim(SUB_QUEUE_CLAIM, subQueue)
        if (expiryInMinutes != null)
        {
            builder.withExpiresAt(Date(Date().time + (expiryInMinutes * 60 * 1000)))
        }
        return builder.sign(algorithm)
    }
}
