package au.kilemon.messagequeue.configuration.cache.redis

import au.kilemon.messagequeue.settings.MessageQueueSettings
import io.lettuce.core.RedisURI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.RedisNode
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.stream.IntStream

/**
 * A test class for [RedisConfiguration] to test any helper methods or initialisation code.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Import(*[RedisConfiguration::class])
class RedisConfigurationTest
{
    /**
     * A Spring configuration that is used for this test class.
     *
     * @author github.com/Kilemonn
     */
    @TestConfiguration
    open class RedisConfigurationTestConfiguration
    {
        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set them here, set them in the [WebMvcTestspring-boot-starter-data-mongodb-test.properties].
         */
        @Bean
        open fun getMessageQueueSettingsBean(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }
    }

    @Autowired
    private lateinit var messageQueueSettings: MessageQueueSettings

    @Autowired
    private lateinit var redisConfiguration: RedisConfiguration

    /**
     * The default port to be used when no port is supplied with the hostname endpoint string.
     */
    private val defaultPort = 22

    /**
     * Ensure that multiple comma separated endpoints can be parsed and properly to the list of addresses.
     */
    @Test
    fun testStringToInetSocketAddresses_multipleAddresses()
    {
        val endpoints = listOf("127.57.99.1", "localhost", "my.endpoint.com")
        val addresses = RedisConfiguration.stringToInetSocketAddresses(endpoints.joinToString(","), defaultPort.toString())

        Assertions.assertEquals(endpoints.size, addresses.size)
        IntStream.range(0, endpoints.size).forEach { index ->
            val endpoint = endpoints[index]
            val address = addresses[index]

            Assertions.assertEquals(endpoint, address.hostName)
            Assertions.assertEquals(defaultPort, address.port)
        }
    }

    /**
     * Ensure that an endpoint and port definition with leading and trailing spaces can be parsed correctly.
     */
    @Test
    fun testStringToInetSocketAddress_withSpaces()
    {
        val endpoint = "55.55.55.55"
        val port = 1
        val fullEndpoint = " $endpoint : $port "
        val address = RedisConfiguration.stringToInetSocketAddresses(fullEndpoint, defaultPort.toString())

        Assertions.assertEquals(1, address.size)
        Assertions.assertEquals(endpoint, address[0].hostName)
        Assertions.assertEquals(port, address[0].port)
    }

    /**
     * Ensure that an endpoint without a provided port will be assigned the provided [defaultPort].
     */
    @Test
    fun testStringToInetSocketAddress_withoutPort()
    {
        val endpoint = "11.11.111.1"
        val address = RedisConfiguration.stringToInetSocketAddresses(endpoint, defaultPort.toString())
        Assertions.assertEquals(1, address.size)
        Assertions.assertEquals(endpoint, address[0].hostName)
        Assertions.assertEquals(defaultPort, address[0].port)
    }

    /**
     * Ensure that an endpoint and port can be parsed correctly.
     */
    @Test
    fun testStringToInetSocketAddress_withPort()
    {
        val endpoint = "11.11.111.1"
        val port = 6780
        val address = RedisConfiguration.stringToInetSocketAddresses("$endpoint:$port", defaultPort.toString())
        Assertions.assertEquals(1, address.size)
        Assertions.assertEquals(endpoint, address[0].hostName)
        Assertions.assertEquals(port, address[0].port)
    }

    /**
     * Ensure that any empty string inputs are not parsed and an empty [List] is returned.
     */
    @Test
    fun testStringToInetSocketAddress_emptyInput()
    {
        var address = RedisConfiguration.stringToInetSocketAddresses("", defaultPort.toString())
        Assertions.assertEquals(0, address.size)

        address = RedisConfiguration.stringToInetSocketAddresses("  ", defaultPort.toString())
        Assertions.assertEquals(0, address.size)
    }

    /**
     * Ensure that a [RedisInitialisationException] is thrown if there is no endpoint provided when sentinels are enabled.
     */
    @Test
    fun testGetConnectionFactory_sentinelWithNoEndpoints()
    {
        messageQueueSettings.redisMode = RedisMode.SENTINEL.name
        messageQueueSettings.redisEndpoint = ""
        Assertions.assertEquals("", messageQueueSettings.redisEndpoint)
        Assertions.assertThrows(RedisInitialisationException::class.java) {
            redisConfiguration.getSentinelConfiguration()
        }
    }

    /**
     * Ensure that a [RedisInitialisationException] is thrown if there is no endpoint provided when standalone mode is enabled.
     */
    @Test
    fun testGetConnectionFactory_standAloneWithNoEndpoints()
    {
        messageQueueSettings.redisMode = RedisMode.STANDALONE.name
        messageQueueSettings.redisEndpoint = ""
        Assertions.assertEquals("", messageQueueSettings.redisEndpoint)
        Assertions.assertThrows(RedisInitialisationException::class.java) {
            redisConfiguration.getStandAloneConfiguration()
        }
    }

    /**
     * Ensure that the first endpoint is used to connect to the redis application when multiple endpoints are provided in standalone mode.
     */
    @Test
    fun testGetConnectionFactory_standAloneWithMultipleEndpoints()
    {
        messageQueueSettings.redisMode = RedisMode.STANDALONE.name
        val endpoint1Host = "localhost"
        val endpoint1Port = "1234"
        val endpoint1 = "$endpoint1Host:$endpoint1Port"
        val endpoint2 = "redis:6789"
        val endpoints = "$endpoint1,$endpoint2"
        messageQueueSettings.redisEndpoint = endpoints
        Assertions.assertEquals(endpoints, messageQueueSettings.redisEndpoint)
        val standAloneConfiguration = redisConfiguration.getStandAloneConfiguration()
        Assertions.assertEquals(endpoint1Host, standAloneConfiguration.hostName)
        Assertions.assertEquals(endpoint1Port.toInt(), standAloneConfiguration.port)
    }

    @Test
    fun testEndpointToNodes_emptyString()
    {
        val endpoint = ""
        val nodes = redisConfiguration.endpointToNodes(endpoint)

        Assertions.assertTrue(nodes.isEmpty())
    }

    @Test
    fun testEndpointToNodes_singleEndpoint()
    {
        val host = "redis-endpoint"
        val port = "1122"
        val endpoint = "$host:$port"
        var nodes = redisConfiguration.endpointToNodes(endpoint)

        Assertions.assertEquals(1, nodes.size)
        Assertions.assertEquals(host, nodes.first().host)
        Assertions.assertEquals(port.toInt(), nodes.first().port)

        nodes = redisConfiguration.endpointToNodes("${RedisURI.URI_SCHEME_REDIS}://$endpoint")
        Assertions.assertEquals(1, nodes.size)
        Assertions.assertEquals(host, nodes.first().host)
        Assertions.assertEquals(port.toInt(), nodes.first().port)
    }

    @Test
    fun testEndpointToNodes_multipleEndpoints()
    {
        val host1 = "redis-endpoint"
        val port = "1122"
        val host2 = "redis-host2"
        val endpoint = "$host1:$port,$host2"
        var nodes = redisConfiguration.endpointToNodes(endpoint)

        Assertions.assertEquals(2, nodes.size)
        Assertions.assertEquals(host1, nodes.first().host)
        Assertions.assertEquals(port.toInt(), nodes.first().port)

        Assertions.assertEquals(2, nodes.size)
        Assertions.assertEquals(host2, nodes[1].host)
        Assertions.assertEquals(RedisNode.DEFAULT_PORT, nodes[1].port)

        nodes = redisConfiguration.endpointToNodes("${RedisURI.URI_SCHEME_REDIS}s://$endpoint")
        Assertions.assertEquals(2, nodes.size)
        Assertions.assertEquals(host1, nodes.first().host)
        Assertions.assertEquals(port.toInt(), nodes.first().port)

        Assertions.assertEquals(2, nodes.size)
        Assertions.assertEquals(host2, nodes[1].host)
        Assertions.assertEquals(RedisNode.DEFAULT_PORT, nodes[1].port)
    }
}
