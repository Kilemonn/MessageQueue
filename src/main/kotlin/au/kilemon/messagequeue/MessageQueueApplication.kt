package au.kilemon.messagequeue

import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.inmemory.InMemoryMultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.MultiQueueType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

/**
 * The main application class.
 *
 * @author github.com/KyleGonzalez
 */
@SpringBootApplication
open class MessageQueueApplication
{
    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

    @Bean
    open fun getMultiQueue(): MultiQueue
    {
        return if (messageQueueSettings.multiQueueType == MultiQueueType.IN_MEMORY)
        {
            InMemoryMultiQueue()
        }
        else
        {
            InMemoryMultiQueue()
        }
    }
}

/**
 * The application entry point.
 *
 * @param args program commandline arguments
 */
fun main(args: Array<String>)
{
    runApplication<MessageQueueApplication>(*args)
}
