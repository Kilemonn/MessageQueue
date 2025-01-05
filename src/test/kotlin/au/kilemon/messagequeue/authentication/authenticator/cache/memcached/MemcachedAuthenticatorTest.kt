package au.kilemon.messagequeue.authentication.authenticator.cache.memcached

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticatorTest
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.configuration.cache.memcached.MemcachedConfiguration
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
 * A test class for [MemcachedAuthenticator].
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=MEMCACHED"])
@Testcontainers
@ContextConfiguration(initializers = [MemcachedAuthenticatorTest.Initializer::class])
@Import(*[LoggingConfiguration::class, MemcachedConfiguration::class, QueueConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class])
class MemcachedAuthenticatorTest: MultiQueueAuthenticatorTest()
{
    companion object
    {
        private const val MEMCACHED_PORT: Int = 11211
        private const val MEMCACHED_CONTAINER: String = "memcached:1.6.34-alpine3.21"

        lateinit var memcache: GenericContainer<*>

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            memcache.stop()
        }
    }

    /**
     * The test initialiser for [MemcachedAuthenticatorTest] to initialise the container and test properties.
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
            memcache = GenericContainer(DockerImageName.parse(MEMCACHED_CONTAINER))
                .withExposedPorts(MEMCACHED_PORT).withReuse(false)
            memcache.start()

            TestPropertyValues.of(
                "${MessageQueueSettings.CACHE_ENDPOINT}=${memcache.host}:${memcache.getMappedPort(MEMCACHED_PORT)}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    /**
     * Check the container is running before each test.
     */
    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(memcache.isRunning)
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }
}
