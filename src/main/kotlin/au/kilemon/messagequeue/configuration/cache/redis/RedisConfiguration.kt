package au.kilemon.messagequeue.configuration.cache.redis

import au.kilemon.messagequeue.authentication.AuthenticationMatrix
import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.cache.redis.RedisCacheKeyManager
import au.kilemon.messagequeue.settings.MessageQueueSettings
import io.lettuce.core.RedisURI
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisNode
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.net.InetSocketAddress


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

    companion object
    {
        /**
         * The default Redis Sentinel port.
         */
        const val REDIS_SENTINEL_DEFAULT_PORT: String = "26379"

        /**
         * The default Redis port.
         */
        const val REDIS_DEFAULT_PORT: String = "6379"

        /**
         * The default Redis gossip port when it is cluster mode.
         */
        const val REDIS_DEFAULT_GOSSIP_PORT: String = "16379"

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
    private lateinit var messageQueueSettings: MessageQueueSettings

    /**
     * Parse the provided [MessageQueueSettings.redisMode] to a [RedisMode], defaulting to [RedisMode.STANDALONE] if it cannot be determined.
     */
    private fun getRedisMode(): RedisMode
    {
        val defaultMode = RedisMode.STANDALONE
        val redisMode = messageQueueSettings.redisMode
        try
        {

            if (redisMode.isNotBlank())
            {
                return RedisMode.valueOf(redisMode.uppercase())
            }
        }
        catch (ex: Exception)
        {
            LOG.warn("No redis mode configured [{}], falling back to default [{}].", redisMode, defaultMode, ex)
        }

        return defaultMode
    }

    /**
     * Create the [RedisConnectionFactory] based on the loaded configuration.
<<<<<<< HEAD
     * If [MessageQueueSettings.redisMode] is `true` then multiple endpoints are expected in [MessageQueueSettings.redisEndpoint] and will attempt to be parsed out
=======
     * If [MessageQueueSettings.redisUseSentinels] is `true` then multiple endpoints are expected in [MessageQueueSettings.cacheEndpoint] and will attempt to be parsed out
>>>>>>> 36e6084 (Work through adding memcached support. Implementing most methods the same way redis is implemented for now.)
     * and set into the [RedisSentinelConfiguration].
     *
     * Otherwise, the first endpoint and port provided will be used to create a [RedisStandaloneConfiguration].
     *
     * @return the created [RedisConnectionFactory] based on the configured [MessageQueueSettings]
     */
    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="REDIS")
    fun getConnectionFactory(): RedisConnectionFactory
    {
        val redisMode = getRedisMode()
        return if (redisMode == RedisMode.SENTINEL)
        {
            LettuceConnectionFactory(getSentinelConfiguration())
        }
        else if (redisMode == RedisMode.CLUSTER)
        {
            LettuceConnectionFactory(getClusterConfiguration())
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
        LOG.info("Initialising redis sentinel configuration with the following configuration: Endpoints {}, master {}. With prefix {}.",
            messageQueueSettings.cacheEndpoint, messageQueueSettings.redisMasterName, messageQueueSettings.cachePrefix)

        val redisSentinelConfiguration = RedisSentinelConfiguration()
        redisSentinelConfiguration.master(messageQueueSettings.redisMasterName)
        val sentinelEndpoints = stringToInetSocketAddresses(messageQueueSettings.cacheEndpoint, REDIS_SENTINEL_DEFAULT_PORT)

        if (sentinelEndpoints.isEmpty())
        {
            LOG.error("No redis endpoints defined for sentinel configuration. Unable to initialise redis configuration.")
            throw RedisInitialisationException("No redis endpoint(s) provided.")
        }

        for (sentinelEndpoint in sentinelEndpoints)
        {
            LOG.debug("Initialising redis sentinel configuration with host {} and port {}.", sentinelEndpoint.hostName, sentinelEndpoint.port)
            redisSentinelConfiguration.sentinel(sentinelEndpoint.hostName, sentinelEndpoint.port)
        }
        return redisSentinelConfiguration
    }

    /**
     * Create an instance of [RedisStandaloneConfiguration] based on the configuration in the [MessageQueueSettings].
     */
    fun getStandAloneConfiguration(): RedisStandaloneConfiguration
    {
        LOG.info("Initialising redis standalone configuration with the following configuration: Endpoint [{}], prefix [{}].",
            messageQueueSettings.cacheEndpoint, messageQueueSettings.cachePrefix)

        val redisConfiguration = RedisStandaloneConfiguration()
        val redisEndpoints = stringToInetSocketAddresses(messageQueueSettings.cacheEndpoint, REDIS_DEFAULT_PORT)
        if (redisEndpoints.isEmpty())
        {
            LOG.error("No redis endpoints defined for standalone configuration. Unable to initialise redis configuration.")
            throw RedisInitialisationException("No redis endpoint(s) provided.")
        }
        else if (redisEndpoints.size > 1)
        {
            LOG.warn("Multiple redis endpoints defined for standalone configuration. Using first provided endpoint: [{}:{}].", redisEndpoints[0].hostName, redisEndpoints[0].port)
        }

        redisConfiguration.hostName = redisEndpoints[0].hostName
        redisConfiguration.port = redisEndpoints[0].port
        return redisConfiguration
    }

    fun getClusterConfiguration(): RedisClusterConfiguration
    {
        LOG.info("Initialising redis cluster configuration with the following configuration: Endpoint [{}], prefix [{}].",
            messageQueueSettings.cacheEndpoint, messageQueueSettings.cachePrefix)

        val configuration = RedisClusterConfiguration()
        val nodes = endpointToNodes(messageQueueSettings.cacheEndpoint)
        configuration.setClusterNodes(nodes)

        return configuration
    }

    fun endpointToNodes(endpoints: String): List<RedisNode>
    {
        val nodes = ArrayList<RedisNode>()
        var endpointString = endpoints

        if (endpointString.startsWith(RedisURI.URI_SCHEME_REDIS + "s://"))
        {
            endpointString = endpointString.removePrefix(RedisURI.URI_SCHEME_REDIS + "s://")
        }
        else if (endpointString.startsWith(RedisURI.URI_SCHEME_REDIS + "://"))
        {
            endpointString = endpointString.removePrefix(RedisURI.URI_SCHEME_REDIS + "://")
        }

        val split = endpointString.split(",")
        for (endpoint in split)
        {
            if (endpoint.isNotBlank())
            {
                nodes.add(RedisNode.fromString(endpoint, RedisNode.DEFAULT_PORT))
            }
        }

        return nodes
    }

    /**
     * Create a [RedisTemplate] to interact with [QueueMessage] from the [getConnectionFactory].
     *
     * @return the [RedisTemplate] used to interface with the [RedisTemplate] cache.
     */
    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="REDIS")
    fun getQueueRedisTemplate(): RedisTemplate<String, QueueMessage>
    {
        val template = RedisTemplate<String, QueueMessage>()
        template.connectionFactory = getConnectionFactory()
        template.keySerializer = StringRedisSerializer()
        return template
    }

    /**
     * Create a [RedisTemplate] to interact with [AuthenticationMatrix] from the [getConnectionFactory].
     *
     * @return the [RedisTemplate] used to interface with the [RedisTemplate] cache.
     */
    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="REDIS")
    fun getAuthMatrixRedisTemplate(): RedisTemplate<String, AuthenticationMatrix>
    {
        val template = RedisTemplate<String, AuthenticationMatrix>()
        template.connectionFactory = getConnectionFactory()
        template.keySerializer = StringRedisSerializer()
        return template
    }

    @Bean(name=["RedisCacheKeyManagerTemplate"])
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="REDIS")
    fun getRedisCacheKeyManagerRedisTemplate(): RedisTemplate<String, String>
    {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = getConnectionFactory()
        template.keySerializer = StringRedisSerializer()
        return template
    }

    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="REDIS")
    fun getRedisCacheKeyManager(): RedisCacheKeyManager
    {
        return RedisCacheKeyManager()
    }
}
