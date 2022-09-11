package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.io.Serializable
import java.util.*

@JsonPropertyOrder("uuid", "queueType", "data")
data class MessageResponse(val message: QueueMessage, val queueType: String = message.type): Serializable
