package au.kilemon.messagequeue.configuration.cache.redis

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

/**
 *
 */
@Configuration
class RedisConfiguration: HasLogger
{
    override val LOG: Logger = initialiseLogger()

    companion object
    {
        const val REDIS_CLIENT_NAME = "Multi-Queue-Client-Name"

        const val REDIS_SENTINEL_DEFAULT_PORT: String = "26379"
    }

    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.MULTI_QUEUE_TYPE], havingValue="REDIS")
//    @ConditionalOnExpression("#{systemEnvironment['${MessageQueueSettings.MULTI_QUEUE_TYPE}'].equals('REDIS')}")
    fun getConnectionFactory(): RedisConnectionFactory
    {
        if (messageQueueSettings.redisUseSentinels.toBoolean())
        {
            LOG.info("Initialising redis sentinel configuration with the following configuration: Endpoints {}, master {}. With prefix {}.",
                messageQueueSettings.redisEndpoint, messageQueueSettings.redisMasterName, messageQueueSettings.redisPrefix)
            val redisSentinelConfiguration = RedisSentinelConfiguration()
            redisSentinelConfiguration.master(messageQueueSettings.redisMasterName)
            val sentinelEndpoints = messageQueueSettings.redisEndpoint.trim().split(",")
            for (sentinelEndpoint in sentinelEndpoints)
            {
                val splitByColon = sentinelEndpoint.trim().split(":")
                if (splitByColon.isNotEmpty())
                {
                    val host = splitByColon[0].trim()
                    var port = REDIS_SENTINEL_DEFAULT_PORT
                    if (splitByColon.size > 1)
                    {
                        port = splitByColon[1].trim()
                    }
                    LOG.debug("Initialising redis sentinel configuration with host {} and port {}.", host, port)
                    redisSentinelConfiguration.sentinel(host, port.toInt())
                }
            }
            return LettuceConnectionFactory(redisSentinelConfiguration)
        }
        else
        {
            LOG.info("Initialising redis configuration with the following configuration: Endpoint {}, port {}. With prefix {}.",
                messageQueueSettings.redisEndpoint, messageQueueSettings.redisPort, messageQueueSettings.redisPrefix)
            val redisConfiguration = RedisStandaloneConfiguration()
            redisConfiguration.hostName = messageQueueSettings.redisEndpoint
            redisConfiguration.port = messageQueueSettings.redisPort.toInt()

            return LettuceConnectionFactory(redisConfiguration)
        }
    }

    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.MULTI_QUEUE_TYPE], havingValue="REDIS")
//    @ConditionalOnExpression("#{systemEnvironment['${MessageQueueSettings.MULTI_QUEUE_TYPE}'].equals('REDIS')}")
    fun getRedisTemplate(): RedisTemplate<String, QueueMessage>
    {
        val template = RedisTemplate<String, QueueMessage>()
        template.connectionFactory = getConnectionFactory()
        return template
    }
}
