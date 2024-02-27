package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.slf4j.MDC

/**
 * An error response object returned when errors occur.
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "message")
data class ErrorResponse(val message: String?, val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID))
