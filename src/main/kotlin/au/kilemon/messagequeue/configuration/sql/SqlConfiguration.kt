package au.kilemon.messagequeue.configuration.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.hibernate.Session
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

/**
 *
 */
@Configuration
class SqlConfiguration: HasLogger
{
    override val LOG: Logger = initialiseLogger()

    companion object
    {
        fun createDatabaseProperties(messageQueueSettings: MessageQueueSettings): Properties
        {
            val properties = Properties()
            properties["hibernate.dialect"] = messageQueueSettings.sqlDialect
            properties["hibernate.connection.driver_class"] = messageQueueSettings.sqlDriver
            properties["hibernate.connection.url"] = messageQueueSettings.sqlEndpoint
            properties["hibernate.connection.username"] = messageQueueSettings.sqlUsername
            properties["hibernate.connection.password"] = messageQueueSettings.sqlPassword
            properties["hibernate.hbm2ddl.auto"] = "create"
            // properties["show_sql"] = "true"
            return properties
        }
    }

    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.MULTI_QUEUE_TYPE], havingValue="SQL")
    fun createHibernateSession(): Session
    {
        val properties = createDatabaseProperties(messageQueueSettings)
        logPropertiesWithoutPassword(properties)
        val configuration = org.hibernate.cfg.Configuration().setProperties(properties)
        return configuration.configure().buildSessionFactory().currentSession
    }

    private fun logPropertiesWithoutPassword(properties: Properties)
    {
        val propertiesWithoutPassword = Properties(properties)
        propertiesWithoutPassword.remove("hibernate.connection.password")
        // Keep the password out of the property map before its logged
        LOG.info("Initialising Sql connection with the following properties: [{}], Password length [{}].", properties.toString(), messageQueueSettings.sqlPassword.length)

    }
}
