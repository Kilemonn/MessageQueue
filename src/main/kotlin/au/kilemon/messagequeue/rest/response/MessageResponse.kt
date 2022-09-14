package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonPropertyOrder

/**
 * @author github.com/KyleGonzalez
 */
@JsonPropertyOrder("queueType", "message")
data class MessageResponse(val message: QueueMessage, val queueType: String = message.type)
