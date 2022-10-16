package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.configuration.cache.redis.RedisConfiguration
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import au.kilemon.messagequeue.settings.MultiQueueType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


/**
 * A test class for the [RedisMultiQueue] Component class.
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=REDIS"])
@Testcontainers
class RedisMultiQueueTest: AbstractMultiQueueTest<RedisMultiQueue>()
{
    @Container
    var redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.0.5-alpine"))
        .withExposedPorts(6379)

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
        lateinit var connectionFactory: RedisConnectionFactory

        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set the here, set them in the [WebMvcTest.properties].
         */
        @Bean
        open fun getMessageQueueSettings(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }

        @Bean
        open fun redisTemplate(): RedisTemplate<String, QueueMessage>
        {
            val template = RedisTemplate<String, QueueMessage>()
            template.connectionFactory = connectionFactory
            return template
        }
    }

    override fun duringSetup()
    {
        Assertions.assertTrue(redis.isRunning)
    }
}
