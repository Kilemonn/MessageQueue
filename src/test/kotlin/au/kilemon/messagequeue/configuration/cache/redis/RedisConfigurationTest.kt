package au.kilemon.messagequeue.configuration.cache.redis

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
}
