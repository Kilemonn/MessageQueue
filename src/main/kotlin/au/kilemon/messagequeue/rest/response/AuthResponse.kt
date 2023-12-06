package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.slf4j.MDC

/**
 * A response object which wraps the response jwt token.
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "subQueue", "token")
class AuthResponse(val token: String, val subQueue: String, val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID))
