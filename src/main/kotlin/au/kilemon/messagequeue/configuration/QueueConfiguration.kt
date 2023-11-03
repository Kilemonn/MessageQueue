package au.kilemon.messagequeue.configuration

import au.kilemon.messagequeue.MessageQueueApplication
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.logging.Messages
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.cache.redis.RedisMultiQueue
import au.kilemon.messagequeue.queue.inmemory.InMemoryMultiQueue
import au.kilemon.messagequeue.queue.nosql.mongo.MongoMultiQueue
import au.kilemon.messagequeue.queue.sql.SqlMultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.cache.redis.RedisAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.inmemory.InMemoryAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.nosql.mongo.MongoAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.sql.SqlAuthenticator
import au.kilemon.messagequeue.authentication.token.JwtTokenProvider
import au.kilemon.messagequeue.settings.MultiQueueType
import lombok.Generated
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.data.redis.core.RedisTemplate
import java.util.*

/**
 * A [Configuration] class holding all required [Bean]s for the [MessageQueueApplication] to run.
 * Mainly [Bean]s related to the [MultiQueue].
 *
 * @author github.com/Kilemonn
 */
@Configuration
class QueueConfiguration : HasLogger
{
    override val LOG: Logger = initialiseLogger()

    @Autowired
    private lateinit var messageQueueSettings: MessageQueueSettings

    @Autowired
    private lateinit var messageSource: ReloadableResourceBundleMessageSource

    @Autowired
    @Lazy
    private lateinit var redisTemplate: RedisTemplate<String, QueueMessage>

    /**
     * Initialise the [MultiQueue] [Bean] based on the [MessageQueueSettings.multiQueueType].
     */
    @Bean
    open fun getMultiQueue(): MultiQueue
    {
        LOG.info(messageSource.getMessage(Messages.VERSION_START_UP, null, Locale.getDefault()), MessageQueueApplication.VERSION)

        // Default to in-memory
        var queue: MultiQueue = InMemoryMultiQueue()
        when (messageQueueSettings.multiQueueType.uppercase()) {
            MultiQueueType.REDIS.toString() -> {
                queue = RedisMultiQueue(messageQueueSettings.redisPrefix, redisTemplate)
            }
            MultiQueueType.SQL.toString() -> {
                queue = SqlMultiQueue()
            }
            MultiQueueType.MONGO.toString() -> {
                queue = MongoMultiQueue()
            }
        }

        LOG.info("Initialising [{}] queue as the [{}] is set to [{}].", queue::class.java.name, MessageQueueSettings.MULTI_QUEUE_TYPE, messageQueueSettings.multiQueueType)

        return queue
    }

    /**
     * Initialise the [MultiQueueAuthenticationType] which drives how sub queues are accessed and created.
     */
    @Bean
    open fun getMultiQueueAuthenticationType(): MultiQueueAuthenticationType
    {
        var authenticationType = MultiQueueAuthenticationType.NONE

        if (messageQueueSettings.multiQueueAuthentication.isNotBlank())
        {
            try
            {
                authenticationType = MultiQueueAuthenticationType.valueOf(messageQueueSettings.multiQueueAuthentication.uppercase())
            }
            catch (ex: Exception)
            {
                LOG.warn("Unable to initialise appropriate authentication type with provided value [{}], falling back to default [{}].", messageQueueSettings.multiQueueAuthentication, MultiQueueAuthenticationType.NONE, ex)
            }
        }

        LOG.info("Using [{}] authentication as the [{}] is set to [{}].", authenticationType, MessageQueueSettings.MULTI_QUEUE_AUTHENTICATION, messageQueueSettings.multiQueueAuthentication)

        return authenticationType
    }

    /**
     * Initialise the [MultiQueueAuthenticator] [Bean] based on the [MessageQueueSettings.multiQueueType].
     */
    @Bean
    open fun getMultiQueueAuthenticator(): MultiQueueAuthenticator
    {
        var authenticator: MultiQueueAuthenticator = InMemoryAuthenticator()
        when (messageQueueSettings.multiQueueType.uppercase()) {
            MultiQueueType.REDIS.toString() -> {
                authenticator = RedisAuthenticator()
            }
            MultiQueueType.SQL.toString() -> {
                authenticator = SqlAuthenticator()
            }
            MultiQueueType.MONGO.toString() -> {
                authenticator = MongoAuthenticator()
            }
        }

        LOG.info("Initialising [{}] authenticator as the [{}] is set to [{}].", authenticator::class.java.name, MessageQueueSettings.MULTI_QUEUE_TYPE, messageQueueSettings.multiQueueType)

        return authenticator
    }

    @Bean
    open fun getJwtTokenProvider(): JwtTokenProvider
    {
        return JwtTokenProvider()
    }
}
