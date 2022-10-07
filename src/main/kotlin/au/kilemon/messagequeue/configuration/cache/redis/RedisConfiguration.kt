package au.kilemon.messagequeue.configuration.cache.redis

import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory

/**
 *
 */
@Configuration
class RedisConfiguration
{
    companion object
    {
        const val REDIS_CLIENT_NAME = "Multi-Queue-Client-Name"
    }

    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

    @Bean
    @ConditionalOnExpression("#{(environment.${MessageQueueSettings.MULTI_QUEUE_TYPE}).equals('REDIS')}")
    fun jedisConnectionFactory(): JedisConnectionFactory?
    {
        val clientConfiguration = JedisClientConfiguration.builder()
        clientConfiguration.clientName(REDIS_CLIENT_NAME)

        if (messageQueueSettings.redisUseSentinels.toBoolean())
        {
            val redisSentinelConfiguration = RedisSentinelConfiguration()
            redisSentinelConfiguration.master(messageQueueSettings.redisMasterName)
        }

        println("Starting redis")
        val jedisConFactory = JedisConnectionFactory()
        jedisConFactory.clientName = REDIS_CLIENT_NAME
        jedisConFactory.hostName = messageQueueSettings.redisEndpoint
        jedisConFactory.port = messageQueueSettings.redisPort.toInt()
        return jedisConFactory
    }
}