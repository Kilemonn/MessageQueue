package au.kilemon.messagequeue.configuration.cache.memcached

import au.kilemon.messagequeue.configuration.cache.redis.RedisConfiguration
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.queue.cache.memcached.MemcachedCacheKeyManager
import au.kilemon.messagequeue.settings.MessageQueueSettings
import net.rubyeye.xmemcached.MemcachedClient
import net.rubyeye.xmemcached.XMemcachedClientBuilder
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration

/**
 * A class that creates the required [Bean] objects when memcached is enabled.
 *
 * @author github.com/Kilemonn
 */
@Configuration
class MemcachedConfiguration: HasLogger
{
    override val LOG: Logger = this.initialiseLogger()

    companion object
    {
        const val MEMCACHED_DEFAULT_PORT: String = "11211"
    }

    @Autowired
    private lateinit var messageQueueSettings: MessageQueueSettings

    /**
     * Create the [MemcachedClient] based on the loaded configuration.
     */
    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="MEMCACHED")
    fun getMemcachedClient(): MemcachedClient
    {
        LOG.info("Initialising memcached configuration with the following configuration: Endpoint(s) [{}]. With prefix [{}].",
            messageQueueSettings.cacheEndpoint, messageQueueSettings.cachePrefix)

        val builder = XMemcachedClientBuilder(RedisConfiguration.stringToInetSocketAddresses(messageQueueSettings.cacheEndpoint, MEMCACHED_DEFAULT_PORT))
        return builder.build()
    }

    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="MEMCACHED")
    fun getMemcachedCacheKeyManager(): MemcachedCacheKeyManager
    {
        return MemcachedCacheKeyManager()
    }
}
