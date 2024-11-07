package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A response object which wraps the [QueueMessage].
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "message")
class MessageResponse
{
    @Schema(description = "The retrieved or created QueueMessage.")
    val message: QueueMessage

    /**
     * Not converting to primary constructor, so we can use [Schema] annotations.
     */
    constructor(message: QueueMessage)
    {
        this.message = message
    }
}
