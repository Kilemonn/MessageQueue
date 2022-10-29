package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.configuration.cache.redis.RedisConfiguration
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisSentinelConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.*

/**
 * A test class for the [RedisMultiQueue] `Component` class running in the Sentinel mode.
 *
 * Very similar to [RedisStandAloneMultiQueueTest], refer to comments there for more detail about the flow of these tests.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=REDIS"])
@Testcontainers
class RedisSentinelMultiQueueTest: AbstractMultiQueueTest<RedisMultiQueue>()
{
    companion object
    {
        private const val REDIS_CONTAINER: String = "redis:7.0.5-alpine"

        private const val REDIS_SENTINEL_CONTAINER: String = "s7anley/redis-sentinel-docker:3.2.12"

        lateinit var redis: GenericContainer<*>

        lateinit var sentinel: GenericContainer<*>

        lateinit var initialProperties: Properties

        /**
         * Force start the container, so we can place its host and dynamic ports into the system properties.
         *
         * Set the environment variables before any of the beans are initialised.
         */
        @BeforeAll
        @JvmStatic
        fun beforeClass()
        {
            redis = GenericContainer(DockerImageName.parse(REDIS_CONTAINER))
                .withExposedPorts(RedisConfiguration.REDIS_DEFAULT_PORT.toInt()).withReuse(false)
            redis.start()

            val envMap = HashMap<String, String>()
            // For the sentinel container to determine where the master node is accessible from
            envMap["MASTER"] = redis.host
            envMap["REDIS_PORT"] = redis.getMappedPort(RedisConfiguration.REDIS_DEFAULT_PORT.toInt()).toString()
            envMap["MASTER_NAME"] = MessageQueueSettings.REDIS_MASTER_NAME_DEFAULT
            sentinel = GenericContainer(DockerImageName.parse(REDIS_SENTINEL_CONTAINER))
                .withExposedPorts(RedisConfiguration.REDIS_SENTINEL_DEFAULT_PORT.toInt()).withReuse(false)
                .withEnv(envMap)
            sentinel.start()

            initialProperties = Properties(System.getProperties())
            val properties = System.getProperties()
            properties[MessageQueueSettings.REDIS_ENDPOINT] = "${sentinel.host}:${sentinel.getMappedPort(RedisConfiguration.REDIS_SENTINEL_DEFAULT_PORT.toInt())}"
            properties[MessageQueueSettings.REDIS_USE_SENTINELS] = true.toString()
            System.setProperties(properties)
        }

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            sentinel.stop()
            redis.stop()

            System.setProperties(initialProperties)
        }
    }

    /**
     * A Spring configuration that is used for this test class.
     * Creates a [RedisSentinelConfiguration] as the [RedisConnectionFactory] [Bean].
     *
     * This is specifically creating the [RedisMultiQueue] to be autowired in the parent
     * class and used in all the tests.
     *
     * @author github.com/KyleGonzalez
     */
    @TestConfiguration
    internal class RedisSentinelTestConfiguration
    {
        @Autowired
        @Lazy
        lateinit var connectionFactory: RedisConnectionFactory

        @Autowired
        @Lazy
        lateinit var messageQueueSettings: MessageQueueSettings

        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set the here, set them in the [WebMvcTest.properties].
         */
        @Bean
        @Lazy
        open fun getMessageQueueSettingsBean(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }

        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set the here, set them in the [WebMvcTest.properties].
         */
        @Bean
        @Lazy
        open fun getRedisMultiQueue(): RedisMultiQueue
        {
            return RedisMultiQueue()
        }

        @Bean
        @Lazy
        open fun getRedisConnectionFactory(): RedisConnectionFactory
        {
            Assertions.assertTrue(messageQueueSettings.redisUseSentinels.toBoolean())
            val redisSentinelConfiguration = RedisSentinelConfiguration()
            redisSentinelConfiguration.master(messageQueueSettings.redisMasterName)
            val sentinelEndpoints = RedisConfiguration.stringToInetSocketAddresses(messageQueueSettings.redisEndpoint, RedisConfiguration.REDIS_SENTINEL_DEFAULT_PORT)
            sentinelEndpoints.forEach{ endpoint -> redisSentinelConfiguration.sentinel(endpoint.hostName, endpoint.port) }

            return LettuceConnectionFactory(redisSentinelConfiguration)
        }

        @Bean
        @Lazy
        open fun redisTemplate(): RedisTemplate<String, QueueMessage>
        {
            val template = RedisTemplate<String, QueueMessage>()
            template.connectionFactory = connectionFactory
            return template
        }
    }

    /**
     * Check the container is running before each test as it's required for the methods to access the [RedisMultiQueue].
     */
    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(redis.isRunning)
        Assertions.assertTrue(sentinel.isRunning)
        multiQueue.clear()
    }
}
