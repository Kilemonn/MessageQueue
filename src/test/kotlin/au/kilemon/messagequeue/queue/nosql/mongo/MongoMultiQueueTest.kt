package au.kilemon.messagequeue.queue.nosql.mongo

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@ExtendWith(SpringExtension::class)
@Testcontainers
@DataMongoTest(properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=MONGO"])
@ContextConfiguration(initializers = [MongoMultiQueueTest.Initializer::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, AbstractMultiQueueTest.AbstractMultiQueueTestConfiguration::class] )
class MongoMultiQueueTest: AbstractMultiQueueTest()
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
            mongoDb.stop()
        }
    }

    /**
     * The test initialiser for [MongoMultiQueueTest] to initialise the container and test properties.
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
            val password = "password"
            val username = "root"
            val envMap = HashMap<String, String>()
            envMap["ME_CONFIG_MONGODB_ADMINPASSWORD"] = password
            envMap["ME_CONFIG_MONGODB_ADMINUSERNAME"] = username

            mongoDb = GenericContainer(DockerImageName.parse(MONGO_CONTAINER))
                .withExposedPorts(MONGO_PORT).withReuse(false).withEnv(envMap)
            mongoDb.start()

            val endpoint = "mongodb://${mongoDb.host}:${mongoDb.getMappedPort(MONGO_PORT)}"

            TestPropertyValues.of(
                "spring.data.mongo.host=$endpoint",
                "spring.data.mongo.username=$username",
                "spring.data.mongo.password=$password",
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
