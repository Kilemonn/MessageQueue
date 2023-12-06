package au.kilemon.messagequeue.configuration

import au.kilemon.messagequeue.MessageQueueApplication
import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.cache.redis.RedisAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.inmemory.InMemoryAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.nosql.mongo.MongoAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.sql.SqlAuthenticator
import au.kilemon.messagequeue.authentication.token.JwtTokenProvider
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.logging.Messages
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.cache.redis.RedisMultiQueue
import au.kilemon.messagequeue.queue.inmemory.InMemoryMultiQueue
import au.kilemon.messagequeue.queue.nosql.mongo.MongoMultiQueue
import au.kilemon.messagequeue.queue.sql.SqlMultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.StorageMedium
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
    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    private lateinit var messageQueueSettings: MessageQueueSettings

    @Autowired
    private lateinit var messageSource: ReloadableResourceBundleMessageSource

    @Autowired
    @Lazy
    private lateinit var redisTemplate: RedisTemplate<String, QueueMessage>

    /**
     * Initialise the [MultiQueue] [Bean] based on the [MessageQueueSettings.storageMedium].
     */
    @Bean
    open fun getMultiQueue(): MultiQueue
    {
        LOG.info(messageSource.getMessage(Messages.VERSION_START_UP, null, Locale.getDefault()), MessageQueueApplication.VERSION)

        // Default to in-memory
        var queue: MultiQueue = InMemoryMultiQueue()
        when (messageQueueSettings.storageMedium.uppercase()) {
            StorageMedium.REDIS.toString() -> {
                queue = RedisMultiQueue(messageQueueSettings.redisPrefix, redisTemplate)
            }
            StorageMedium.SQL.toString() -> {
                queue = SqlMultiQueue()
            }
            StorageMedium.MONGO.toString() -> {
                queue = MongoMultiQueue()
            }
        }

        LOG.info("Initialising [{}] queue as the [{}] is set to [{}].", queue::class.java.name, MessageQueueSettings.STORAGE_MEDIUM, messageQueueSettings.storageMedium)

        return queue
    }

    /**
     * Initialise the [RestrictionMode] which drives how sub-queues are accessed and created.
     */
    @Bean
    open fun getRestrictionMode(): RestrictionMode
    {
        val restrictionMode = parseRestrictionMode(messageQueueSettings.restrictionMode)
        LOG.info("Using [{}] restriction mode as the [{}] is set to [{}].", restrictionMode, MessageQueueSettings.RESTRICTION_MODE, messageQueueSettings.restrictionMode)

        return restrictionMode
    }

    /**
     * Parse the provided [String] into a [RestrictionMode]. If it does not match any [RestrictionMode] then [RestrictionMode.NONE] is returned.
     */
    internal fun parseRestrictionMode(restrictionMode: String): RestrictionMode
    {
        val defaultRestrictionMode = RestrictionMode.NONE
        try
        {
            if (restrictionMode.isNotBlank())
            {
                return RestrictionMode.valueOf(restrictionMode.uppercase())
            }
        }
        catch (ex: Exception)
        {
            LOG.warn("Unable to initialise appropriate restriction mode with provided value [{}], falling back to default [{}].", restrictionMode, defaultRestrictionMode, ex)
        }

        return defaultRestrictionMode
    }

    /**
     * Initialise the [MultiQueueAuthenticator] [Bean] based on the [MessageQueueSettings.storageMedium].
     */
    @Bean
    open fun getMultiQueueAuthenticator(): MultiQueueAuthenticator
    {
        var authenticator: MultiQueueAuthenticator = InMemoryAuthenticator()
        when (messageQueueSettings.storageMedium.uppercase()) {
            StorageMedium.REDIS.toString() -> {
                authenticator = RedisAuthenticator()
            }
            StorageMedium.SQL.toString() -> {
                authenticator = SqlAuthenticator()
            }
            StorageMedium.MONGO.toString() -> {
                authenticator = MongoAuthenticator()
            }
        }

        LOG.info("Initialising [{}] authenticator as the [{}] is set to [{}].", authenticator::class.java.name, MessageQueueSettings.STORAGE_MEDIUM, messageQueueSettings.storageMedium)

        return authenticator
    }

    @Bean
    open fun getJwtTokenProvider(): JwtTokenProvider
    {
        return JwtTokenProvider()
    }
}
