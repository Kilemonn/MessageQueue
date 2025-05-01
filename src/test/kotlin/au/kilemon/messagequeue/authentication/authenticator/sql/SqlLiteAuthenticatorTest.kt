package au.kilemon.messagequeue.authentication.authenticator.sql

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticatorTest
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
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
import org.testcontainers.junit.jupiter.Testcontainers


/**
 * A test class for [SqlAuthenticator] using SQLLite as the backing database.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Testcontainers
@DataJpaTest(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=SQL", "spring.jpa.hibernate.ddl-auto=create", "spring.autoconfigure.exclude="])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [SqlLiteAuthenticatorTest.Initializer::class])
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class SqlLiteAuthenticatorTest: MultiQueueAuthenticatorTest()
{
    /**
     * The test initialiser for [SqlLiteAuthenticatorTest] to initialise the container and test properties.
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
            val password = "sa" // container has password requirements
            val username = "sa"

            val endpoint = "jdbc:sqlite:memory:myDb?cache=shared"

            TestPropertyValues.of(
                "spring.datasource.url=$endpoint",
                "spring.datasource.username=$username",
                "spring.datasource.password=$password",

                // We have to explicitly set the dialect here since without it spring is unable to determine
                // the dialect to use when we run all tests.
                "spring.jpa.properties.hibernate.dialect=org.hibernate.community.dialect.SQLiteDialect"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    @BeforeEach
    fun beforeEach()
    {
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }
}