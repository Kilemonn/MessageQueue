package au.kilemon.messagequeue.filter

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
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
    }

    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    lateinit var authenticationType: MultiQueueAuthenticationType

    @Autowired
    lateinit var authenticator: MultiQueueAuthenticator

    /**
     *
     */
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain)
    {
        try
        {
            val subQueue = getSubQueueInToken(request)
            setSubQueue(subQueue)

            if (authenticationType == MultiQueueAuthenticationType.NONE)
            {
                LOG.trace("Allowed access as authentication is set to [{}].", MultiQueueAuthenticationType.NONE)
                filterChain.doFilter(request, response)
            }
            else if (authenticationType == MultiQueueAuthenticationType.HYBRID)
            {
                LOG.trace("Allowing request through for lower layer to check as authentication is set to [{}].", MultiQueueAuthenticationType.NONE)
                filterChain.doFilter(request, response)
            }
            else if (authenticationType == MultiQueueAuthenticationType.RESTRICTED)
            {
                if (subQueue.isPresent && authenticator.isRestricted(subQueue.get()))
                {
                    LOG.trace("Accepted request for sub queue [{}].", subQueue.get())
                    filterChain.doFilter(request, response)
                }
                else
                {
                    LOG.error("Failed to manipulate sub queue [{}] with provided token as the authentication level is set to [{}].", subQueue.get(), authenticationType)
                    // TODO throw here
                }
            }
        }
        finally
        {
            MDC.remove(SUB_QUEUE)
        }
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
     *
     */
    fun getSubQueueInToken(request: HttpServletRequest): Optional<String>
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
    fun isValidJwtToken(jwtToken: String): Optional<String>
    {
        return Optional.ofNullable(null)
    }
}
