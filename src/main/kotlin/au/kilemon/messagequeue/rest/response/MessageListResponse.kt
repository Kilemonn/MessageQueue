package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.message.QueueMessage
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Response object used when multiple messages are being returned.
 *
 * @author github.com/Kilemonn
 */
class MessageListResponse
{
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