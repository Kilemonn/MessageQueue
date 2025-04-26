package au.kilemon.messagequeue.authentication.authenticator.sql

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticatorTest
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
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
 * A test class for [SqlAuthenticator] using Microsoft SQL as the backing database.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Testcontainers
@DataJpaTest(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=SQL", "spring.jpa.hibernate.ddl-auto=create", "spring.autoconfigure.exclude="])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [MicrosoftSqlAuthenticatorTest.Initializer::class])
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class MicrosoftSqlAuthenticatorTest: MultiQueueAuthenticatorTest()
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
     * The test initialiser for [MicrosoftSqlAuthenticatorTest] to initialise the container and test properties.
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

            // "jdbc:sqlserver://localhost:1433;databaseName=dbname;encrypt=false"
            val endpoint = "jdbc:sqlserver://${database.host}:${database.getMappedPort(MS_CONTAINER_PORT)};encrypt=true;trustServerCertificate=true"

            TestPropertyValues.of(
                "spring.datasource.url=$endpoint",
                "spring.datasource.username=$username",
                "spring.datasource.password=$password",

                // We have to explicitly set the dialect here since without it spring is unable to determine
                // the dialect to use when we run all tests.
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    @BeforeEach
    fun beforeEach()
    {
        Assertions.assertTrue(database.isRunning)
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }
}