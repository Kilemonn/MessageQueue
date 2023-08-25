package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.slf4j.MDC

/**
 * A response object which wraps the [QueueMessage], and exposes the `type` [String].
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "queueType", "message")
data class MessageResponse(val message: QueueMessage, val queueType: String = message.type, val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID))
