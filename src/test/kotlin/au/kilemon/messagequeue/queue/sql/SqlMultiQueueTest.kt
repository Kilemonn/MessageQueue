package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.configuration.sql.SqlConfiguration
import au.kilemon.messagequeue.queue.AbstractMultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.hibernate.Session
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*


@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = ["${MessageQueueSettings.MULTI_QUEUE_TYPE}=SQL"])
@Testcontainers
class SqlMultiQueueTest: AbstractMultiQueueTest<SqlMultiQueue>()
{
    companion object
    {
//        lateinit var redis: GenericContainer<*>

        lateinit var initialProperties: Properties

        /**
         * Force start the container, so we can place its host and dynamic ports into the system properties.
         *
         * Set the environment variables before any of the beans are initialised.
         */
        @BeforeAll
        @JvmStatic
        fun beforeClass()
        {
//            redis = GenericContainer(DockerImageName.parse(REDIS_CONTAINER))
//                .withExposedPorts(REDIS_PORT).withReuse(false)
//            redis.start()

            initialProperties = Properties(System.getProperties())
            val properties = System.getProperties()
            properties[MessageQueueSettings.SQL_DRIVER] = "org.postgresql.Driver"
            properties[MessageQueueSettings.SQL_DIALECT] = "org.hibernate.dialect.PostgreSQLDialect"
            properties[MessageQueueSettings.SQL_ENDPOINT] = "jdbc:postgresql://127.0.0.1:5432/postgres"
            properties[MessageQueueSettings.SQL_USERNAME] = "postgres"
            properties[MessageQueueSettings.SQL_PASSWORD] = "password"
            System.setProperties(properties)
        }

        /**
         * Stop the container at the end of all the tests.
         */
        @AfterAll
        @JvmStatic
        fun afterClass()
        {
//            redis.stop()

            System.setProperties(initialProperties)
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

        @Bean
        @Lazy
        open fun createHibernateSession(): Session
        {
            val properties = SqlConfiguration.createDatabaseProperties(messageQueueSettings)
            val configuration = org.hibernate.cfg.Configuration().setProperties(properties)
            return configuration.configure().buildSessionFactory().currentSession
        }

//        @Bean
//        @Lazy
//        fun getEntityFactory(): LocalContainerEntityManagerFactoryBean
//        {
//            val manager = LocalContainerEntityManagerFactoryBean()
//
//            return manager
//        }
    }

    /**
     * Check the container is running before each test as it's required for the methods to access the [SqlMultiQueue].
     */
    @BeforeEach
    fun beforeEach()
    {
//        Assertions.assertTrue(redis.isRunning)
        multiQueue.clear()
    }
}
