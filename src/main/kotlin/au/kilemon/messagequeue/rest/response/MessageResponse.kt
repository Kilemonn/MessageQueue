package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonPropertyOrder

/**
 * A response object which wraps the [QueueMessage], and exposes the `type` [String].
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("queueType", "message")
data class MessageResponse(val message: QueueMessage, val queueType: String = message.type)
