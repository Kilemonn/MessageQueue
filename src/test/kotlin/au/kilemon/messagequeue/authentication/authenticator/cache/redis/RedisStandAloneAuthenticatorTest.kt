package au.kilemon.messagequeue.authentication.authenticator.cache.redis

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticatorTest
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.configuration.cache.redis.RedisConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
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

/**
 * A test class for [RedisAuthenticator] running in a standalone configuration.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=REDIS", "${MessageQueueSettings.REDIS_PREFIX}=test"])
@Testcontainers
@ContextConfiguration(initializers = [RedisStandAloneAuthenticatorTest.Initializer::class])
@Import(*[QueueConfiguration::class, LoggingConfiguration::class, RedisConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class])
class RedisStandAloneAuthenticatorTest: MultiQueueAuthenticatorTest()
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
     * The test initialiser for [RedisStandAloneAuthenticatorTest] to initialise the container and test properties.
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
                "${MessageQueueSettings.REDIS_ENDPOINT}=${redis.host}:${redis.getMappedPort(REDIS_PORT)}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(redis.isRunning)
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }
}
