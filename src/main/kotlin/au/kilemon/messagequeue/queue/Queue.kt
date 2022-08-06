package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.message.Message
import au.kilemon.messagequeue.message.MessageType
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Component
open class Queue<T: Serializable>
{
    val messageQueue: ConcurrentHashMap<MessageType, Queue<Message<T>>> = ConcurrentHashMap()
}
