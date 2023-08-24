package au.kilemon.messagequeue.filter

import au.kilemon.messagequeue.logging.HasLogger
import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class CorrelationIdFilter: OncePerRequestFilter(), HasLogger
{
    companion object
    {
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"

        const val CORRELATION_LOG_PARAMETER = "cId"
    }

    override val LOG: Logger = initialiseLogger()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain)
    {
        try
        {
            val correlationId: String
            val providedId: String? = request.getHeader(CORRELATION_ID_HEADER)
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

            MDC.put(CORRELATION_LOG_PARAMETER, correlationId)

            filterChain.doFilter(request, response)
        }
        finally
        {
            MDC.clear()
        }
    }
}
