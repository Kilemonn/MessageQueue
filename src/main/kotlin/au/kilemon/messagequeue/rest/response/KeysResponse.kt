package au.kilemon.messagequeue.rest.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * A response object for the keys request.
 *
 * @author github.com/Kilemonn
 */
class KeysResponse
{
    @Schema(description = "A list of sub-queue identifiers that are present in the multi-queue.")
    var keys: Set<String>

    constructor(keys: Set<String>)
    {
        this.keys = keys
    }
}
