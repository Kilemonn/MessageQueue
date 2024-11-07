package au.kilemon.messagequeue.rest.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Response object holding a map of the owners that current hold messages in the multi-queue.
 *
 * @author github.com/Kilemonn
 */
class OwnersMapResponse
{
    @Schema(description = "A map of the assignee as the key and the value is a list of the sub-queues that they have at least one message in.")
    val owners: Map<String, HashSet<String>>

    constructor(owners: Map<String, HashSet<String>>)
    {
        this.owners = owners
    }
}
