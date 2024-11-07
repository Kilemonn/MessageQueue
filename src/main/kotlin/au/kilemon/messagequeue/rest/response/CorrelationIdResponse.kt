package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.MDC

/**
 * A response object which only returns the correlationID.
 *
 * @author github.com/Kilemonn
 */
class CorrelationIdResponse
{
    @Schema(title = "The request correlation ID.", example = "1599dcd3-7424-4f97-bc99-b9b3e5c53d59",
        description = "A UUID that uniquely identifies the performed request. This will correlate with any logs written as part of this request for debugging purposes.")
    val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID)
}