package au.kilemon.messagequeue.authentication.authenticator.nosql.mongo

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticatorTest
import au.kilemon.messagequeue.authentication.authenticator.nosql.mongo.MongoMultiAuthenticatorTest.Companion.MONGO_CONTAINER
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
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

/**
 * A test class for the [MONGO_CONTAINER] to ensure the [MongoAuthenticator] works as expected with this underlying data
 * storage DB.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Testcontainers
@DataMongoTest(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=MONGO"])
@ContextConfiguration(initializers = [MongoMultiAuthenticatorTest.Initializer::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class MongoMultiAuthenticatorTest: MultiQueueAuthenticatorTest()
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
     * The test initialiser for [MongoMultiAuthenticatorTest] to initialise the container and test properties.
     *
     * @author github.com/Kilemonn
     */
    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext>
    {
        /**
         * Force start the container, so we can place its host and dynamic ports into the system properties.
         *
         * Set the environment variables before any of the beans are initialised.
         *
         * The following properties can also be used:
         * - "spring.data.mongodb.host=${mongoDb.host}"
         * - "spring.data.mongodb.database=$databaseName"
         * - "spring.data.mongodb.username=$username"
         * - "spring.data.mongodb.password=$password"
         * - "spring.data.mongodb.port=${mongoDb.getMappedPort(MONGO_PORT)}"
         */
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

            val databaseName = "MultiQueue"
            // mongodb://<username>:<password>@<host>:<port>/<database>
            val endpoint = "mongodb://$username:$password@${mongoDb.host}:${mongoDb.getMappedPort(MONGO_PORT)}/$databaseName?authSource=admin"

            TestPropertyValues.of(
                "spring.data.mongodb.uri=$endpoint"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(mongoDb.isRunning)
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }
}
