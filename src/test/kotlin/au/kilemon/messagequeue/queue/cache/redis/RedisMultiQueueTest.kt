package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Import
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
@Import(RedisStandAloneTestConfiguration::class)
@Testcontainers
class RedisMultiQueueTest: AbstractMultiQueueTest<RedisMultiQueue>()
{
    @Container
    var redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.0.5-alpine"))
        .withExposedPorts(6379)

    override fun duringSetup()
    {
        Assertions.assertTrue(redis.isRunning)
    }
}
