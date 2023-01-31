package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
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
import java.util.HashMap

/**
 * A test class for the [MYSQL_CONTAINER] to ensure the [SqlMultiQueue] works as expected with this underlying data storage DB.
 *
 * @author github.com/KyleGonzalez
 */
@ContextConfiguration(initializers = [MySqlMultiQueueTest.Initializer::class])
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, AbstractMultiQueueTest.AbstractMultiQueueTestConfiguration::class] )
class MySqlMultiQueueTest : AbstractSqlMultiQueueTest()
{
    companion object
    {
        lateinit var database: GenericContainer<*>

        private const val MYSQL_CONTAINER = "mysql:8.0.31"
        private const val MYSQL_PORT = 3306

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
     * The test initialiser for [MySqlMultiQueueTest] to initialise the container and test properties.
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
            envMap["MYSQL_ROOT_PASSWORD"] = password

            database = GenericContainer(DockerImageName.parse(MYSQL_CONTAINER))
                .withExposedPorts(MYSQL_PORT).withReuse(false).withEnv(envMap)
            database.start()

            val endpoint = "jdbc:mysql://${database.host}:${database.getMappedPort(MYSQL_PORT)}/mysql"
            val username = "root"

            TestPropertyValues.of(
                "spring.datasource.url=$endpoint",
                "spring.datasource.username=$username",
                "spring.datasource.password=$password",
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    /**
     * Check the container is running before each test as it's required for the methods to access the [MySqlMultiQueueTest].
     */
    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(database.isRunning)
        multiQueue.clear()
    }
}
