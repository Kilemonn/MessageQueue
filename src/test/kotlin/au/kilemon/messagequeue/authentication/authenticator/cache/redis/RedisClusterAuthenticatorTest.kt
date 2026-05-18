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
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.Inet4Address
import java.net.NetworkInterface
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
class RedisClusterAuthenticatorTest: MultiQueueAuthenticatorTest()
{
    companion object
    {
        private const val REDIS_CONTAINER: String = "redis:7.2.14-alpine"

        const val AMOUNT_OF_INSTANCES = 6
        val redisInstances: ArrayList<GenericContainer<*>> = ArrayList()

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            redisInstances.forEach { it.stop() }
        }

        /**
         * Get the host IP address that is reachable from the docker containers.
         *
         * @return the host IP address or "host.docker.internal" as a fallback
         */
        fun getHostIp(): String
        {
            return NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filter { it is Inet4Address }
                .map { it.hostAddress }
                .firstOrNull() ?: "host.docker.internal"
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
            val hostIp = getHostIp()
            val startCommand = "redis-server --cluster-enabled yes"
            IntStream.range(0, AMOUNT_OF_INSTANCES).forEach {
                val instance = GenericContainer(DockerImageName.parse(REDIS_CONTAINER))
                    .withExposedPorts(RedisConfiguration.REDIS_DEFAULT_PORT.toInt(), RedisConfiguration.REDIS_DEFAULT_GOSSIP_PORT.toInt())
                    .withReuse(false)
                    .withCommand(startCommand)
                instance.start()
                redisInstances.add(instance)

                val mappedPort = instance.getMappedPort(RedisConfiguration.REDIS_DEFAULT_PORT.toInt())
                val mappedBusPort = instance.getMappedPort(RedisConfiguration.REDIS_DEFAULT_GOSSIP_PORT.toInt())

                var result = instance.execInContainer("redis-cli", "CONFIG", "SET", "cluster-announce-ip", hostIp)
                Assertions.assertEquals("OK", result.stdout.trim())

                result = instance.execInContainer("redis-cli", "CONFIG", "SET", "cluster-announce-port", "$mappedPort")
                Assertions.assertEquals("OK", result.stdout.trim())

                result = instance.execInContainer("redis-cli", "CONFIG", "SET", "cluster-announce-bus-port", "$mappedBusPort")
                Assertions.assertEquals("OK", result.stdout.trim())

                result = instance.execInContainer("redis-cli", "CONFIG", "SET", "loglevel", "debug")
                Assertions.assertEquals("OK", result.stdout.trim())
            }

            Thread.sleep(5000)

            val clusterStartCommand = mutableListOf("redis-cli", "--cluster", "create")

            redisInstances.forEach {
                val mappedPort = it.getMappedPort(RedisConfiguration.REDIS_DEFAULT_PORT.toInt())
                clusterStartCommand.add("$hostIp:$mappedPort")
            }

            clusterStartCommand.add("--cluster-replicas")
            clusterStartCommand.add("1")
            clusterStartCommand.add("--cluster-yes")

            val result = redisInstances[0].execInContainer(*clusterStartCommand.toTypedArray())
            Assertions.assertEquals(0, result.exitCode, "Cluster creation failed: ${result.stderr}")

            // Sleep while the cluster is initialised
            Thread.sleep(20000)

            val endpoints = StringBuilder()
            redisInstances.forEach { endpoints.append("$hostIp:${it.getMappedPort(RedisConfiguration.REDIS_DEFAULT_PORT.toInt())},") }
            TestPropertyValues.of(
                "${MessageQueueSettings.CACHE_ENDPOINT}=${endpoints}",
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
