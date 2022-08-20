package au.kilemon.messagequeue.rest.response

import java.io.Serializable
import java.util.*

data class MessageResponse(val uuid: UUID = UUID.randomUUID(), var queueType: String): Serializable
