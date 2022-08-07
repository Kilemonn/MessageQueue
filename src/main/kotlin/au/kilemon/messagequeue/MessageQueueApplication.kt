package au.kilemon.messagequeue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * The main application class.
 *
 * @author github.com/KyleGonzalez
 */
@SpringBootApplication
open class MessageQueueApplication

/**
 * The application entry point.
 *
 * @param args program commandline arguments
 */
fun main(args: Array<String>)
{
    runApplication<MessageQueueApplication>(*args)
}
