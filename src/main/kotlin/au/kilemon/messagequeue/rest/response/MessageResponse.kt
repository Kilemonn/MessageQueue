package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.MDC

/**
 * A response object which wraps the [QueueMessage], and exposes the `sub-queue` [String].
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "message")
class MessageResponse
{
    @Schema(title = "The request correlation ID.", example = "1599dcd3-7424-4f97-bc99-b9b3e5c53d59")
    val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID)

    val message: QueueMessage

    /**
     * Not converting to primary constructor, so we can use [Schema] annotations.
     */
    constructor(message: QueueMessage)
    {
        this.message = message
    }
}
