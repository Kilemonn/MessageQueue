package au.kilemon.messagequeue.queue.sql

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.*


/**
 * A test class for the [POSTGRES_CONTAINER] to ensure the [SqlMultiQueue] works as expected with this underlying data storage DB.
 *
 * @author github.com/KyleGonzalez
 */
@ContextConfiguration(initializers = [PostgreSqlMultiQueueTest.Initializer::class])
@Import(AbstractSqlMultiQueueTest.SqlMultiQueueTestConfiguration::class)
class PostgreSqlMultiQueueTest: AbstractSqlMultiQueueTest()
{
    companion object
    {
        lateinit var database: GenericContainer<*>

        private const val POSTGRES_CONTAINER = "postgres:14.5"
        private const val POSTGRES_PORT = 5432

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
            database.stop()
        }
    }

    /**
     * The test initialiser for [PostgreSqlMultiQueueTest] to initialise the container and test properties.
     *
     * @author github.com/KyleGonzalez
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
            val envMap = HashMap<String, String>()
            envMap["POSTGRES_PASSWORD"] = password

            database = GenericContainer(DockerImageName.parse(POSTGRES_CONTAINER))
                .withExposedPorts(POSTGRES_PORT).withReuse(false).withEnv(envMap)
            database.start()

            val endpoint = "jdbc:postgresql://${database.host}:${database.getMappedPort(POSTGRES_PORT)}/postgres"
            val username = "postgres"

            TestPropertyValues.of(
                "spring.datasource.url=$endpoint",
                "spring.datasource.username=$username",
                "spring.datasource.password=$password",
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    /**
     * Check the container is running before each test as it's required for the methods to access the [SqlMultiQueue].
     */
    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(database.isRunning)
        multiQueue.clear()
    }
}
