package au.kilemon.messagequeue.queue.nosql.mongo

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.queue.nosql.mongo.MongoMultiQueueTest.Companion.MONGO_CONTAINER
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * A test class for the [MONGO_CONTAINER] to ensure the [MongoMultiQueue] works as expected with this underlying data storage DB.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Testcontainers
@DataMongoTest(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=MONGO"])
@ContextConfiguration(initializers = [MongoMultiQueueTest.Initializer::class])
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class, MongoMultiQueueTest.MongoTestConfig::class] )
class MongoMultiQueueTest: MultiQueueTest()
{
    companion object
    {
        lateinit var mongoDb: GenericContainer<*>

        private const val MONGO_CONTAINER = "mongo:7.0.0"
        private const val MONGO_PORT = 27017

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            if (::mongoDb.isInitialized)
            {
                mongoDb.stop()
            }
        }
    }

    @TestConfiguration
    open class MongoTestConfig
    {
        @Bean
        open fun mongoClient(): MongoClient
        {
            val host = mongoDb.host
            val port = mongoDb.getMappedPort(MONGO_PORT)
            val endpoint = "mongodb://root:password@$host:$port/MultiQueue?authSource=admin"
            return MongoClients.create(endpoint)
        }
    }

    /**
     * The test initialiser for [MongoMultiQueueTest] to initialise the container and test properties.
     *
     * @author github.com/Kilemonn
     */
    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext>
    {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext)
        {
            val password = "password"
            val username = "root"
            val envMap = HashMap<String, String>()
            envMap["MONGO_INITDB_ROOT_PASSWORD"] = password
            envMap["MONGO_INITDB_ROOT_USERNAME"] = username

            mongoDb = GenericContainer(DockerImageName.parse(MONGO_CONTAINER))
                .withExposedPorts(MONGO_PORT).withReuse(false).withEnv(envMap)
            mongoDb.start()

            val host = mongoDb.host
            val port = mongoDb.getMappedPort(MONGO_PORT)
            val databaseName = "MultiQueue"
            // mongodb://<username>:<password>@<host>:<port>/<database>
            val endpoint = "mongodb://$username:$password@$host:$port/$databaseName?authSource=admin"

            TestPropertyValues.of(
                "spring.data.mongodb.uri=$endpoint"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    /**
     * Check the container is running before each test as it's required for the methods to access the [MongoMultiQueue].
     */
    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(mongoDb.isRunning)
        multiQueue.clear()
    }
}
