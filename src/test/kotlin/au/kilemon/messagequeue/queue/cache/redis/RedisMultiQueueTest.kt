package au.kilemon.messagequeue.queue.cache.redis

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
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


/**
 * A test class for the [RedisMultiQueue] `Component` class.
 *
 * This class relies on a lot of beans being [Lazy], so we can set up the [System] `Properties` before they are properly used.
 * Other implementation classes could break these tests. Keep this in mind when reviewing broken tests.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=REDIS"])
@Testcontainers
class RedisMultiQueueTest: AbstractMultiQueueTest<RedisMultiQueue>()
{
    companion object
    {
        private const val REDIS_PORT: Int = 6379

        lateinit var redis: GenericContainer<*>

        /**
         * Force start the container, so we can place its host and dynamic ports into the system properties.
         *
         * Set the environment variables before any of the beans are initialised.
         */
        @BeforeAll
        @JvmStatic
        fun beforeClass()
        {
            redis = GenericContainer(DockerImageName.parse("redis:7.0.5-alpine"))
                .withExposedPorts(REDIS_PORT).withReuse(false)
            redis.start()

            val properties = System.getProperties()
            properties[MessageQueueSettings.REDIS_ENDPOINT] = redis.host
            properties[MessageQueueSettings.REDIS_PORT] = redis.getMappedPort(REDIS_PORT).toString()
            System.setProperties(properties)
        }

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            redis.stop()
        }
    }

    /**
     * A Spring configuration that is used for this test class.
     *
     * This is specifically creating the [RedisMultiQueue] to be autowired in the parent
     * class and used in all the tests.
     *
     * @author github.com/KyleGonzalez
     */
    @TestConfiguration
    internal class RedisStandAloneTestConfiguration
    {
        @Autowired
        @Lazy
        lateinit var connectionFactory: RedisConnectionFactory

        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set the here, set them in the [WebMvcTest.properties].
         */
        @Bean
        @Lazy
        open fun getMessageQueueSettings(): MessageQueueSettings
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
            val redisConfiguration = RedisStandaloneConfiguration()
            redisConfiguration.hostName = getMessageQueueSettings().redisEndpoint
            redisConfiguration.port = getMessageQueueSettings().redisPort.toInt()

            return LettuceConnectionFactory(redisConfiguration)
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
    }
}