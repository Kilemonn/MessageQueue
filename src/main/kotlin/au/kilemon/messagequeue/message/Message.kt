package au.kilemon.messagequeue.message

import java.io.Serializable

data class Message<T: Serializable>(val data: T?): Serializable
