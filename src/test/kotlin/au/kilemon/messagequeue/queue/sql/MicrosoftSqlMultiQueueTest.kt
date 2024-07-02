package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.queue.sql.MicrosoftSqlMultiQueueTest.Companion.MS_SQL_CONTAINER
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

/**
 * A test class for the [MS_SQL_CONTAINER] to ensure the [SqlMultiQueue] works as expected with this underlying data storage DB.
 *
 * @author github.com/Kilemonn
 */
@ContextConfiguration(initializers = [MicrosoftSqlMultiQueueTest.Initializer::class])
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class MicrosoftSqlMultiQueueTest: SqlMultiQueueTest()
{
    companion object
    {
        lateinit var database: GenericContainer<*>

        private const val MS_SQL_CONTAINER = "mcr.microsoft.com/mssql/server:2022-CU13-ubuntu-22.04"
        private const val MS_CONTAINER_PORT = 1433

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
     * The test initialiser for [MicrosoftSqlMultiQueueTest] to initialise the container and test properties.
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
            val password = "This154Strong" // container has password requirements
            val username = "sa"
            val envMap = HashMap<String, String>()
            envMap["MSSQL_SA_PASSWORD"] = password
            envMap["ACCEPT_EULA"] = "Y"

            database = GenericContainer(DockerImageName.parse(MS_SQL_CONTAINER))
                .withExposedPorts(MS_CONTAINER_PORT).withReuse(false).withEnv(envMap)
            database.start()

            // "jdbc:sqlserver://localhost:1433;databaseName=dbname;user=username;password=*****;"
            val endpoint = "jdbc:sqlserver://${database.host}:${database.getMappedPort(MS_CONTAINER_PORT)};encrypt=false"

            TestPropertyValues.of(
                "spring.datasource.url=$endpoint",
                "spring.datasource.username=$username",
                "spring.datasource.password=$password",
                "hibernate.dialect=org.hibernate.dialect.SQLServerDialect"
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