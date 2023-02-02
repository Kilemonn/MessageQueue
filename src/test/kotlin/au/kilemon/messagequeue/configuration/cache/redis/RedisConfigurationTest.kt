package au.kilemon.messagequeue.configuration.cache.redis

import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.stream.IntStream

/**
 * A test class for [RedisConfiguration] to test any helper methods or initialisation code.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
class RedisConfigurationTest
{
    /**
     * A Spring configuration that is used for this test class.
     *
     * @author github.com/KyleGonzalez
     */
    @TestConfiguration
    open class RedisConfigurationTestConfiguration
    {
        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set them here, set them in the [WebMvcTest.properties].
         */
        @Bean
        open fun getMessageQueueSettingsBean(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }
    }

    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

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
        messageQueueSettings.redisUseSentinels = "true"
        messageQueueSettings.redisEndpoint = ""
        Assertions.assertTrue(messageQueueSettings.redisUseSentinels.toBoolean())
        Assertions.assertEquals("", messageQueueSettings.redisEndpoint)
        Assertions.assertThrows(RedisInitialisationException::class.java) {
            val config = RedisConfiguration()
            config.messageQueueSettings = messageQueueSettings
            config.getSentinelConfiguration()
        }
    }

    /**
     * Ensure that a [RedisInitialisationException] is thrown if there is no endpoint provided when standalone mode is enabled.
     */
    @Test
    fun testGetConnectionFactory_standAloneWithNoEndpoints()
    {
        messageQueueSettings.redisUseSentinels = "false"
        messageQueueSettings.redisEndpoint = ""
        Assertions.assertFalse(messageQueueSettings.redisUseSentinels.toBoolean())
        Assertions.assertEquals("", messageQueueSettings.redisEndpoint)
        Assertions.assertThrows(RedisInitialisationException::class.java) {
            val config = RedisConfiguration()
            config.messageQueueSettings = messageQueueSettings
            config.getStandAloneConfiguration()
        }
    }

    /**
     * Ensure that the first endpoint is used to connect to the redis application when multiple endpoints are provided in standalone mode.
     */
    @Test
    fun testGetConnectionFactory_standAloneWithMultipleEndpoints()
    {
        messageQueueSettings.redisUseSentinels = "false"
        val endpoint1Host = "localhost"
        val endpoint1Port = "1234"
        val endpoint1 = "$endpoint1Host:$endpoint1Port"
        val endpoint2 = "redis:6789"
        val endpoints = "$endpoint1,$endpoint2"
        messageQueueSettings.redisEndpoint = endpoints
        Assertions.assertFalse(messageQueueSettings.redisUseSentinels.toBoolean())
        Assertions.assertEquals(endpoints, messageQueueSettings.redisEndpoint)
        val config = RedisConfiguration()
        config.messageQueueSettings = messageQueueSettings
        val standAloneConfiguration = config.getStandAloneConfiguration()
        Assertions.assertEquals(endpoint1Host, standAloneConfiguration.hostName)
        Assertions.assertEquals(endpoint1Port.toInt(), standAloneConfiguration.port)
    }
}
