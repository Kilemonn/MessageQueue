package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

/**
* A test class for SqlLite to ensure the [SqlMultiQueue] works as expected with this underlying data storage DB.
*
* @author github.com/Kilemonn
*/
@ContextConfiguration(initializers = [SqlLiteMultiQueueTest.Initializer::class])
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class SqlLiteMultiQueueTest: SqlMultiQueueTest()
{
    /**
     * The test initialiser for [SqlLiteMultiQueueTest] to initialise the container and test properties.
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

    /**
     * Check the container is running before each test as it's required for the methods to access the [SqlMultiQueue].
     */
    @BeforeEach
    fun beforeEach()
    {
        multiQueue.clear()
    }
}
