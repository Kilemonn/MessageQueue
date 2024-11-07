package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import au.kilemon.messagequeue.message.QueueMessage
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.MDC

/**
 * Response object used when multiple messages are being returned.
 *
 * @author github.com/Kilemonn
 */
class MessageListResponse {
    @Schema(title = "The request correlation ID.", example = "1599dcd3-7424-4f97-bc99-b9b3e5c53d59",
        description = "A UUID that uniquely identifies the performed request. This will correlate with any logs written as part of this request for debugging purposes.")
    val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID)

    @Schema(description = "The retrieved or created QueueMessages.")
    val messages: List<QueueMessage>

    /**
     * Not converting to primary constructor, so we can use [Schema] annotations.
     */
    constructor(messages: List<QueueMessage>)
    {
        this.messages = messages
    }
}