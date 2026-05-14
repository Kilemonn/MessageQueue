package au.kilemon.messagequeue.authentication.authenticator.cache.redis

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticatorTest
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.configuration.cache.redis.RedisConfiguration
import au.kilemon.messagequeue.configuration.cache.redis.RedisMode
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
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.stream.IntStream

/**
 * A test class for [RedisAuthenticator] running in a cluster configuration.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=REDIS"])
@Testcontainers
@ContextConfiguration(initializers = [RedisClusterAuthenticatorTest.Initializer::class])
@Import(*[LoggingConfiguration::class, RedisConfiguration::class, QueueConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class])
class RedisClusterAuthenticatorTest : MultiQueueAuthenticatorTest()
{
    companion object
    {
        private const val REDIS_CONTAINER: String = "redis:7.2.14-alpine"

        const val AMOUNT_OF_INSTANCES = 6
        val redisInstances: ArrayList<GenericContainer<*>> = ArrayList()
        val redisNetwork: Network = Network.newNetwork()

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            redisInstances.forEach { it.stop() }
            redisNetwork.close()
        }
    }

    /**
     * The test initialiser for [RedisClusterAuthenticatorTest] to initialise the container and test properties.
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
            val nodeNamePrefix = "redis-node-"
            val startCommand = "redis-server --cluster-enabled yes --cluster-announce-hostname "
            IntStream.range(0, AMOUNT_OF_INSTANCES).forEach {
                val instance = GenericContainer(DockerImageName.parse(REDIS_CONTAINER))
                    .withNetwork(redisNetwork)
                    .withNetworkAliases("$nodeNamePrefix$it")
                    .withExposedPorts(RedisConfiguration.REDIS_DEFAULT_PORT.toInt(), RedisConfiguration.REDIS_DEFAULT_PORT.toInt() + 10000)
                    .withReuse(false)
                    .withCommand(startCommand + nodeNamePrefix + it.toString())
                instance.start()
                redisInstances.add(instance)

//                val busPort = instance.getMappedPort(10000 + RedisConfiguration.REDIS_DEFAULT_PORT.toInt())
//                var result = instance.execInContainer("redis-cli", "CONFIG", "SET", "cluster-announce-bus-port", "$busPort")
//                Assertions.assertEquals("OK", result.stdout.trim())
//
//                val exposedPort = instance.getMappedPort(RedisConfiguration.REDIS_DEFAULT_PORT.toInt())
//                result = instance.execInContainer("redis-cli", "CONFIG", "SET", "cluster-announce-port", "$exposedPort")
//                Assertions.assertEquals("OK", result.stdout.trim())

                var result = instance.execInContainer("redis-cli", "CONFIG", "SET", "loglevel", "debug")
                Assertions.assertEquals("OK", result.stdout.trim())
            }

            Thread.sleep(5000)

            val clusterStartCommand = StringBuilder("redis-cli --cluster create")

            redisInstances.forEach { clusterStartCommand.append(" ${it.networkAliases[1]}:${RedisConfiguration.REDIS_DEFAULT_PORT}") }

            clusterStartCommand.append(" --cluster-replicas 0 --cluster-yes")

            val clusterCreator = GenericContainer(DockerImageName.parse(REDIS_CONTAINER))
                .withNetwork(redisNetwork)
                .withCommand(clusterStartCommand.toString())
            clusterCreator.start()

            // Sleep while the cluster is initialised
            Thread.sleep(60000)

            val endpoints = StringBuilder()
            redisInstances.forEach { endpoints.append("host.docker.internal:${it.getMappedPort(RedisConfiguration.REDIS_DEFAULT_PORT.toInt())},") }
            TestPropertyValues.of(
                "${MessageQueueSettings.REDIS_ENDPOINT}=${endpoints}",
                "${MessageQueueSettings.REDIS_MODE}=${RedisMode.CLUSTER.name}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    @BeforeEach
    fun beforeEach()
    {
        redisInstances.forEach { Assertions.assertTrue(it.isRunning) }
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }
}
