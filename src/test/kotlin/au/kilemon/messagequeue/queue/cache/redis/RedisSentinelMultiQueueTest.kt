package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.configuration.cache.redis.RedisConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
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
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=REDIS"])
@Testcontainers
@ContextConfiguration(initializers = [RedisSentinelMultiQueueTest.Initializer::class])
@Import(*[LoggingConfiguration::class, RedisConfiguration::class, QueueConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class])
class RedisSentinelMultiQueueTest: MultiQueueTest()
{
    companion object
    {
        private const val REDIS_CONTAINER: String = "redis:7.2.3-alpine"
        private const val REDIS_SENTINEL_CONTAINER: String = "s7anley/redis-sentinel-docker:3.2.12"

        lateinit var redis: GenericContainer<*>
        lateinit var sentinel: GenericContainer<*>

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            sentinel.stop()
            redis.stop()
        }
    }

    /**
     * The test initialiser for [RedisSentinelMultiQueueTest] to initialise the container and test properties.
     *
     * @author github.com/Kilemonn
     */
    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext>
    {
        /**
         * Force start the container, so we can place its host and dynamic ports into the system properties.
         *
         * Set the environment variables before any of the beans are initialised.
         */
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext)
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

            TestPropertyValues.of(
                "${MessageQueueSettings.REDIS_ENDPOINT}=${sentinel.host}:${sentinel.getMappedPort(RedisConfiguration.REDIS_SENTINEL_DEFAULT_PORT.toInt())}",
                "${MessageQueueSettings.REDIS_USE_SENTINELS}=true"
            ).applyTo(configurableApplicationContext.environment)
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

    /**
     * Test [RedisMultiQueue.removePrefix] to make sure no change is made to the provided [Set] when [RedisMultiQueue.hasPrefix] is false.
     */
    @Test
    fun testRemovePrefix_noPrefix()
    {
        Assertions.assertTrue(multiQueue is RedisMultiQueue)
        val redisMultiQueue: RedisMultiQueue = (multiQueue as RedisMultiQueue)
        Assertions.assertFalse(redisMultiQueue.hasPrefix())

        val subQueue = "removePrefix"
        val subQueue2 = "removePrefix2"
        Assertions.assertTrue(redisMultiQueue.add(QueueMessage("data", subQueue)))
        Assertions.assertTrue(redisMultiQueue.add(QueueMessage("data2", subQueue2)))

        val keys = redisMultiQueue.keys()
        Assertions.assertTrue(keys.contains(subQueue))
        Assertions.assertTrue(keys.contains(subQueue2))

        val removedPrefix = redisMultiQueue.removePrefix(keys)
        Assertions.assertTrue(removedPrefix.containsAll(keys))
    }
}
