package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.MDC

/**
 * Response object holding a map of the owners that current hold messages in the multi-queue.
 *
 * @author github.com/Kilemonn
 */
class OwnersMapResponse
{
    @Schema(title = "The request correlation ID.", example = "1599dcd3-7424-4f97-bc99-b9b3e5c53d59",
        description = "A UUID that uniquely identifies the performed request. This will correlate with any logs written as part of this request for debugging purposes.")
    val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID)

    @Schema(description = "A map of the assignee as the key and the value is a list of the sub-queues that they have at least one message in.")
    val owners: Map<String, HashSet<String>>

    constructor(owners: Map<String, HashSet<String>>)
    {
        this.owners = owners
    }
}