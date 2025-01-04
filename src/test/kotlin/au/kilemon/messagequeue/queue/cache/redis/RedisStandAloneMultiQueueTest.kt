package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.configuration.cache.redis.RedisConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


/**
 * A test class for the [RedisMultiQueue] `Component` class.
 *
 * Because the `SpringContext` and related beans in [TestConfiguration] are initialised before the [BeforeAll] we need to
 * mark a lot of the beans as [Lazy] to ensure that nothing is initialised until the container is initialised and its host and port
 * are placed into the [System] `Properties`.
 * Once this is done the tests will run and the lazy loading will initialise the required beans.
 *
 * Other implementation classes could break these tests. Keep this in mind when reviewing broken tests.
 * This could also impact how the application actually runs since everything is [Lazy] initialised, so it could cause potentially some
 * delay when initialising the beans in some scenarios. At the moment everything is generally small, so it should be okay.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=REDIS", "${MessageQueueSettings.CACHE_PREFIX}=test"])
@Testcontainers
@ContextConfiguration(initializers = [RedisStandAloneMultiQueueTest.Initializer::class])
@Import(*[QueueConfiguration::class, LoggingConfiguration::class, RedisConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class])
class RedisStandAloneMultiQueueTest: MultiQueueTest()
{
    companion object
    {
        private const val REDIS_PORT: Int = 6379
        private const val REDIS_CONTAINER: String = "redis:7.2.3-alpine"

        lateinit var redis: GenericContainer<*>

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
     * The test initialiser for [RedisStandAloneMultiQueueTest] to initialise the container and test properties.
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
                .withExposedPorts(REDIS_PORT).withReuse(false)
            redis.start()

            TestPropertyValues.of(
                "${MessageQueueSettings.CACHE_ENDPOINT}=${redis.host}:${redis.getMappedPort(REDIS_PORT)}"
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
        multiQueue.clear()
    }

    /**
     * Test [RedisMultiQueue.removePrefix] to make sure the prefix is removed correctly.
     */
    @Test
    fun testRemovePrefix()
    {
        Assertions.assertTrue(multiQueue is RedisMultiQueue)
        val redisMultiQueue: RedisMultiQueue = (multiQueue as RedisMultiQueue)
        Assertions.assertTrue(redisMultiQueue.hasPrefix())

        val prefix = redisMultiQueue.getPrefix()

        val subQueue = "removePrefix"
        val subQueue2 = "removePrefix2"
        Assertions.assertTrue(redisMultiQueue.add(QueueMessage("data", subQueue)))
        Assertions.assertTrue(redisMultiQueue.add(QueueMessage("data2", subQueue2)))

        val keys = redisMultiQueue.keys()
        Assertions.assertTrue(keys.contains("$prefix$subQueue"))
        Assertions.assertTrue(keys.contains("$prefix$subQueue2"))
        keys.forEach { key -> Assertions.assertTrue(key.startsWith(prefix)) }

        val removedPrefix = redisMultiQueue.removePrefix(keys)
        Assertions.assertFalse(removedPrefix.contains("$prefix$subQueue"))
        Assertions.assertFalse(removedPrefix.contains("$prefix$subQueue2"))
        removedPrefix.forEach { key -> Assertions.assertFalse(key.startsWith(prefix)) }

        Assertions.assertTrue(removedPrefix.contains(subQueue))
        Assertions.assertTrue(removedPrefix.contains(subQueue2))
    }
}
