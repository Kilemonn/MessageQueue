package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.slf4j.MDC

/**
 * A response object which wraps the [QueueMessage], and exposes the `sub-queue` [String].
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "subQueue", "message")
data class MessageResponse(val message: QueueMessage, val subQueue: String = message.subQueue, val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID))
