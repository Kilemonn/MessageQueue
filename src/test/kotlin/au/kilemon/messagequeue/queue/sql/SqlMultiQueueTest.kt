package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.*


/**
 *
 * Properties to assist with debugging:
 * - "hibernate.show_sql=true"
 * - "hibernate.format_sql=true",
 * - "logging.level.org.hibernate.type=TRACE",
 * - "spring.jpa.show-sql=true",
 * - "spring.jpa.properties.hibernate.format_sql=true"
 *
 * @author github.com/KyleGonzalez
 */
@ExtendWith(SpringExtension::class)
@Testcontainers
@DataJpaTest(properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=SQL", "spring.jpa.hibernate.ddl-auto=create"])
@ContextConfiguration(initializers = [SqlMultiQueueTest.Initializer::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LoggingConfiguration::class)
class SqlMultiQueueTest: AbstractMultiQueueTest<SqlMultiQueue>()
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
     * The test initialiser for [SqlMultiQueueTest] to initialise the container and test properties.
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
     * A Spring configuration that is used for this test class.
     *
     * This is specifically creating the [SqlMultiQueue] to be autowired in the parent
     * class and used in all the tests.
     *
     * @author github.com/KyleGonzalez
     */
    @TestConfiguration
    internal class SqlMultiQueueTestConfiguration
    {
        @Autowired
        @Lazy
        lateinit var messageQueueSettings: MessageQueueSettings

        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set them here, set them in the [WebMvcTest.properties].
         */
        @Bean
        @Lazy
        open fun getMessageQueueSettingsBean(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }

        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set them here, set them in the [WebMvcTest.properties].
         */
        @Bean
        @Lazy
        open fun getSqlMultiQueue(): SqlMultiQueue
        {
            return SqlMultiQueue()
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
