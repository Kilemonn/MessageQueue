package au.kilemon.messagequeue.filter

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthenticationException
import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 *
 *
 * @author github.com/Kilemonn
 */
@Component
@Order(2)
class JwtAuthenticationFilter: OncePerRequestFilter(), HasLogger
{
    companion object
    {
        const val AUTHORIZATION_HEADER = "Authorization"

        const val SUB_QUEUE = "Sub-Queue"

        /**
         * Gets the stored [SUB_QUEUE] from the [MDC].
         * This can be null if no valid token is provided.
         */
        fun getSubQueue(): String?
        {
            return MDC.get(SUB_QUEUE)
        }
    }

    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    lateinit var authenticator: MultiQueueAuthenticator

    /**
     *
     */
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain)
    {
        try
        {
            val subQueue = getSubQueueInTokenFromHeaders(request)
            setSubQueue(subQueue)

            if (authenticator.isInNoneMode())
            {
                LOG.trace("Allowed access as authentication is set to [{}].", MultiQueueAuthenticationType.NONE)
                filterChain.doFilter(request, response)
            }
            else if (authenticator.isInHybridMode())
            {
                LOG.trace("Allowing request through for lower layer to check as authentication is set to [{}].", MultiQueueAuthenticationType.NONE)
                filterChain.doFilter(request, response)
            }
            else if (authenticator.isInRestrictedMode())
            {
                if (tokenIsPresentAndQueueIsRestricted(subQueue, authenticator))
                {
                    LOG.trace("Accepted request for sub queue [{}].", subQueue.get())
                    filterChain.doFilter(request, response)
                }
                else
                {
                    LOG.error("Failed to manipulate sub queue [{}] with provided token as the authentication level is set to [{}].", subQueue.get(), authenticator.getAuthenticationType())
                    throw MultiQueueAuthenticationException()
                }
            }
        }
        finally
        {
            MDC.remove(SUB_QUEUE)
        }
    }

    /**
     *
     */
    fun tokenIsPresentAndQueueIsRestricted(subQueue: Optional<String>, multiQueueAuthenticator: MultiQueueAuthenticator): Boolean
    {
        return subQueue.isPresent && multiQueueAuthenticator.isRestricted(subQueue.get())
    }

    /**
     * Set the provided [Optional][String] into the [MDC] as [JwtAuthenticationFilter.SUB_QUEUE] if it is not [Optional.empty].
     *
     * @param subQueue an optional sub queue identifier, if it is not [Optional.empty] it will be placed into the [MDC]
     */
    fun setSubQueue(subQueue: Optional<String>)
    {
        if (subQueue.isPresent)
        {
            LOG.trace("Setting resolved sub queue from token into request context [{}].", subQueue.get())
            MDC.put(SUB_QUEUE, subQueue.get())
        }
    }

    /**
     * Get the value of the provided [request] for the [AUTHORIZATION_HEADER] header.
     *
     * @param request the request to retrieve the [AUTHORIZATION_HEADER] from
     * @return the [AUTHORIZATION_HEADER] header value wrapped as an [Optional], otherwise [Optional.empty]
     */
    fun getSubQueueInTokenFromHeaders(request: HttpServletRequest): Optional<String>
    {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)
        if (authHeader != null)
        {
            return isValidJwtToken(authHeader)
        }
        return Optional.empty()
    }

    /**
     *
     */
    @Throws(MultiQueueAuthenticationException::class)
    fun isValidJwtToken(jwtToken: String): Optional<String>
    {
        return Optional.ofNullable(jwtToken)
    }
}
