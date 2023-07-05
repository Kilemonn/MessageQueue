package au.kilemon.messagequeue.configuration.cache.redis

import au.kilemon.messagequeue.logging.ExceptionMessages
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.logging.Messages
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.settings.MessageQueueSettings
import lombok.Generated
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.net.InetSocketAddress
import java.util.*
import kotlin.collections.ArrayList


/**
 * A class that creates the required [Bean] objects when redis is enabled.
 * This will create either a standalone configuration or a sentinel configuration based on the provided properties.
 *
 * @author github.com/Kilemonn
 */
@Configuration
class RedisConfiguration: HasLogger
{
    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    @get:Generated
    @set:Generated
    lateinit var messageSource: ReloadableResourceBundleMessageSource

    companion object
    {
        /**
         * The default Redis Sentinel port.
         */
        const val REDIS_SENTINEL_DEFAULT_PORT: String = "26379"

        /**
         * The default Redis port for standalone connections.
         */
        const val REDIS_DEFAULT_PORT: String = "6379"

        /**
         * A helper method which takes a comma separated list of endpoints and optionally ports in the format: `<endpoint>:<port>,<endpoint2>`.
         * This method will parse them and return a [List] of [InetSocketAddress].
         *
         * If there is no port provided, then the provided [defaultPort] will be used.
         *
         * @param endpoints the string of comma separated endpoints and optional ports
         * @param defaultPort the default port to use for any endpoint that does not explicitly provide one
         * @return a list of [InetSocketAddress] matching the contents of the input comma separated list
         */
        fun stringToInetSocketAddresses(endpoints: String, defaultPort: String): List<InetSocketAddress>
        {
            val list = ArrayList<InetSocketAddress>()

            val splitEndpoints = endpoints.trim().split(",")
            for (endpoint in splitEndpoints)
            {
                if (endpoint.isNotBlank())
                {
                    val splitByColon = endpoint.trim().split(":")
                    if (splitByColon.isNotEmpty())
                    {
                        val host = splitByColon[0].trim()
                        var port = defaultPort
                        if (splitByColon.size > 1)
                        {
                            port = splitByColon[1].trim()
                        }
                        list.add(InetSocketAddress.createUnresolved(host, port.toInt()))
                    }
                }
            }

            return list
        }
    }

    @Autowired
    @get:Generated
    @set:Generated
    lateinit var messageQueueSettings: MessageQueueSettings

    /**
     * Create the [RedisConnectionFactory] based on the loaded configuration.
     * If [MessageQueueSettings.redisUseSentinels] is `true` then multiple endpoints are expected in [MessageQueueSettings.redisEndpoint] and will attempt to be parsed out
     * and set into the [RedisSentinelConfiguration].
     *
     * Otherwise, the first endpoint and port provided will be used to create a [RedisStandaloneConfiguration].
     *
     * @return the created [RedisConnectionFactory] based on the configured [MessageQueueSettings]
     */
    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.MULTI_QUEUE_TYPE], havingValue="REDIS")
    fun getConnectionFactory(): RedisConnectionFactory
    {
        return if (messageQueueSettings.redisUseSentinels.toBoolean())
        {
            LettuceConnectionFactory(getSentinelConfiguration())
        }
        else
        {
            LettuceConnectionFactory(getStandAloneConfiguration())
        }
    }

    /**
     * Create an instance of [RedisSentinelConfiguration] based on the configuration in the [MessageQueueSettings].
     */
    fun getSentinelConfiguration(): RedisSentinelConfiguration
    {
        LOG.info(messageSource.getMessage(Messages.RedisConfiguration.Sentinel.INITIALISING, arrayOf(messageQueueSettings.redisEndpoint, messageQueueSettings.redisMasterName, messageQueueSettings.redisPrefix), Locale.getDefault()))
        val redisSentinelConfiguration = RedisSentinelConfiguration()
        redisSentinelConfiguration.master(messageQueueSettings.redisMasterName)
        val sentinelEndpoints = stringToInetSocketAddresses(messageQueueSettings.redisEndpoint, REDIS_SENTINEL_DEFAULT_PORT)

        if (sentinelEndpoints.isEmpty())
        {
            LOG.error(messageSource.getMessage(Messages.RedisConfiguration.Sentinel.NO_ENDPOINT, null, Locale.getDefault()))
            throw RedisInitialisationException(messageSource.getMessage(ExceptionMessages.RedisConfiguration.Sentinel.NO_ENDPOINT, null, Locale.getDefault()))
        }

        for (sentinelEndpoint in sentinelEndpoints)
        {
            LOG.debug(messageSource.getMessage(Messages.RedisConfiguration.Sentinel.INITIALISING_WITH_HOST_AND_PORT, arrayOf(sentinelEndpoint.hostName, sentinelEndpoint.port), Locale.getDefault()))
            redisSentinelConfiguration.sentinel(sentinelEndpoint.hostName, sentinelEndpoint.port)
        }
        return redisSentinelConfiguration
    }

    /**
     * Create an instance of [RedisStandaloneConfiguration] based on the configuration in the [MessageQueueSettings].
     */
    fun getStandAloneConfiguration(): RedisStandaloneConfiguration
    {
        LOG.info(messageSource.getMessage(Messages.RedisConfiguration.Standalone.INITIALISING, arrayOf(messageQueueSettings.redisEndpoint, messageQueueSettings.redisPrefix), Locale.getDefault()))
        val redisConfiguration = RedisStandaloneConfiguration()
        val redisEndpoints = stringToInetSocketAddresses(messageQueueSettings.redisEndpoint, REDIS_DEFAULT_PORT)
        if (redisEndpoints.isEmpty())
        {
            LOG.error(messageSource.getMessage(Messages.RedisConfiguration.Standalone.NO_ENDPOINT, null, Locale.getDefault()))
            throw RedisInitialisationException(messageSource.getMessage(ExceptionMessages.RedisConfiguration.Standalone.NO_ENDPOINT, null, Locale.getDefault()))
        }
        else if (redisEndpoints.size > 1)
        {
            LOG.warn(messageSource.getMessage(Messages.RedisConfiguration.Standalone.MULTIPLE_ENDPOINTS, arrayOf(redisEndpoints[0].hostName, redisEndpoints[0].port), Locale.getDefault()))
        }

        redisConfiguration.hostName = redisEndpoints[0].hostName
        redisConfiguration.port = redisEndpoints[0].port
        return redisConfiguration
    }

    /**
     * Create the [RedisTemplate] from [getConnectionFactory].
     *
     * @return the [RedisTemplate] used to interface with the [RedisTemplate] cache.
     */
    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.MULTI_QUEUE_TYPE], havingValue="REDIS")
    fun getRedisTemplate(): RedisTemplate<String, QueueMessage>
    {
        val template = RedisTemplate<String, QueueMessage>()
        template.connectionFactory = getConnectionFactory()
        template.keySerializer = StringRedisSerializer()
        return template
    }
}
