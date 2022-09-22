package au.kilemon.messagequeue

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.inmemory.InMemoryMultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.MultiQueueType
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ReloadableResourceBundleMessageSource

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
         * The `resource` path to the `messages.properties` file that holds external source messages.
         */
        const val SOURCE_MESSAGES: String = "classpath:messages"
    }

    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

    /**
     * Initialise the [MultiQueue] [Bean] based on the [MessageQueueSettings.multiQueueType].
     */
    @Bean
    open fun getMultiQueue(): MultiQueue
    {
        return if (messageQueueSettings.multiQueueType == MultiQueueType.IN_MEMORY)
        {
            LOG.debug("Initialising [{}] queue as the [{}] is set to [{}].", InMemoryMultiQueue::class.java.name, MessageQueueSettings.MULTI_QUEUE_TYPE, MultiQueueType.IN_MEMORY)
            InMemoryMultiQueue()
        }
        else
        {
            LOG.warn("Initialising [{}] queue as the [{}] is set to [{}].", InMemoryMultiQueue::class.java.name, MessageQueueSettings.MULTI_QUEUE_TYPE, "null")
            InMemoryMultiQueue()
        }
    }

    /**
     * Initialise the [MessageSource] [Bean] to read from [MessageQueueApplication.SOURCE_MESSAGES].
     */
    @Bean
    fun getMessageSource(): MessageSource
    {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename(SOURCE_MESSAGES)
        LOG.debug("Initialising message source for resource [{}].", SOURCE_MESSAGES)
        return messageSource
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
