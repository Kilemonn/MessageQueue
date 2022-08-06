package au.kilemon.messagequeue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class MessageQueueApplication

fun main(args: Array<String>)
{
    runApplication<MessageQueueApplication>(*args)
}
