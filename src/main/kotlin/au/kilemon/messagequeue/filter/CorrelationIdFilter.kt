package au.kilemon.messagequeue.filter

import au.kilemon.messagequeue.logging.HasLogger
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*


/**
 * A request filter that either takes the incoming provided [CORRELATION_ID_HEADER] and sets it into the [MDC] OR
 * generates a new [UUID] that is used as the [CORRELATION_ID] for this request.
 * This [CORRELATION_ID] is removed from the [MDC] when the request is returned to the caller.
 *
 * This also ensures that [CORRELATION_ID_HEADER] is set into the response headers too.
 *
 * @author github.com/Kilemonn
 */
@Component
@Order(1)
class CorrelationIdFilter: OncePerRequestFilter(), HasLogger
{
    companion object
    {
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"

        // This is also used in the logback.xml as a parameter
        const val CORRELATION_ID = "correlationId"
    }

    override val LOG: Logger = this.initialiseLogger()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain)
    {
        try
        {
            setCorrelationId(request.getHeader(CORRELATION_ID_HEADER))

            filterChain.doFilter(request, response)
            response.setHeader(CORRELATION_ID_HEADER, MDC.get(CORRELATION_ID))
        }
        finally
        {
            MDC.remove(CORRELATION_ID)
        }
    }

    /**
     * Handle the setting of the [CORRELATION_ID] based on the [providedId] if it is not null it will be used, otherwise
     * a new [UUID] will be generated and set into the [MDC].
     *
     * @param providedId the correlation ID provided by the user, if it is null a new one will be generated
     */
    fun setCorrelationId(providedId: String?)
    {
        val correlationId: String
        if (providedId != null)
        {
            correlationId = providedId
            LOG.trace("Using provided ID [{}] as correlation id.", correlationId)
        }
        else
        {
            correlationId = UUID.randomUUID().toString()
            LOG.trace("Using generated UUID [{}] as correlation id.", correlationId)
        }

        MDC.put(CORRELATION_ID, correlationId)
    }
}
