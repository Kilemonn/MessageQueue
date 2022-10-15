package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.MultiQueueType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConfiguration
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

/**
 * A Spring configuration that is used for this test class.
 *
 * This is specifically creating the [RedisMultiQueue] to be autowired in the parent
 * class and used in all the tests.
 *
 * @author github.com/KyleGonzalez
 */
@TestConfiguration
open class RedisStandAloneTestConfiguration
{
    @Bean
    open fun getMessageQueueSettings(): MessageQueueSettings
    {
        val settings = MessageQueueSettings()
        settings.multiQueueType = MultiQueueType.REDIS.toString()
        return settings
    }

    @Bean
    open fun getJedisConnectionFactory(): JedisConnectionFactory
    {
        val redisConfiguration = RedisStandaloneConfiguration()
        redisConfiguration.hostName = getMessageQueueSettings().redisEndpoint
        redisConfiguration.port = getMessageQueueSettings().redisPort.toInt()
        return JedisConnectionFactory(redisConfiguration)
    }

    @Bean
    open fun redisTemplate(): RedisTemplate<String, Set<QueueMessage>>
    {
        val template = RedisTemplate<String, Set<QueueMessage>>()
        template.connectionFactory = getJedisConnectionFactory()
        return template
    }

    @Bean
    open fun getRedisMultiQueue(): RedisMultiQueue
    {
        return RedisMultiQueue()
    }
}
