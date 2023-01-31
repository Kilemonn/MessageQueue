package au.kilemon.messagequeue

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.logging.Messages
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.cache.redis.RedisMultiQueue
import au.kilemon.messagequeue.queue.inmemory.InMemoryMultiQueue
import au.kilemon.messagequeue.queue.sql.SqlMultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.MultiQueueType
import lombok.Generated
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import java.util.*

/**
 * The main application class.
 *
 * @author github.com/KyleGonzalez
 */
@SpringBootApplication
open class MessageQueueApplication : HasLogger
{
    override val LOG: Logger = initialiseLogger()

    companion object
    {
        /**
         * Application version number, make sure this matches what is defined in `build.gradle.kts`.
         */
        const val VERSION: String = "0.1.6"
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
