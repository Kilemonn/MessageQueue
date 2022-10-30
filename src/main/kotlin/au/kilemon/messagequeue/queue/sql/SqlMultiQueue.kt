package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.postgresql.util.PSQLException
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

/**
 *
 * @author github.com/KyleGonzalez
 */
class SqlMultiQueue : MultiQueue, HasLogger
{
    override val LOG: Logger = initialiseLogger()

    @Autowired
    @Lazy
    lateinit var messageQueueSettings: MessageQueueSettings

    private lateinit var sqlType: SqlType

    private lateinit var connection: Connection

    init
    {
        initialiseDatabaseConnection()
        checkAndInitialiseDatabaseTables()
    }

    private fun initialiseDatabaseConnection()
    {
        try
        {
            val type = SqlType.fromDriverName(messageQueueSettings.sqlDriver)
            if (type == null)
            {
                val errorMessage = String.format("Failed to recognise provided driver name [%s] with any of the supported drivers.", messageQueueSettings.sqlDriver)
                throw SqlInitialisationException(errorMessage)
            }
            sqlType = type
            Class.forName(messageQueueSettings.sqlDriver)
        }
        catch (ex: Exception)
        {
            val errorMessage = String.format("Failed to load driver with name [%s], please make sure it is a supported driver and is spelt correctly.", messageQueueSettings.sqlDriver)
            LOG.error(errorMessage)
            throw SqlInitialisationException(errorMessage, ex)
        }

        try
        {
            connection = DriverManager.getConnection(messageQueueSettings.sqlEndpoint, messageQueueSettings.sqlUsername, messageQueueSettings.sqlPassword)
        }
        catch (ex: Exception)
        {
            val errorMessage = String.format("Failed to instantiate connection to Sql database using, driver: [%s], endpoint: [%s], username: [%s], password length: [%d].",
                messageQueueSettings.sqlDriver, messageQueueSettings.sqlEndpoint, messageQueueSettings.sqlUsername, messageQueueSettings.sqlPassword.length)
            LOG.error(errorMessage)
            throw SqlInitialisationException(errorMessage, ex)
        }
    }

    private fun checkAndInitialiseDatabaseTables()
    {
        val checkTableExists = "SELECT 1 FROM ?"
        connection.prepareStatement(checkTableExists).use { statement ->
            statement.setString(1, MessageQueueSettings.SQL_TABLE_NAME_DEFAULT)
            try
            {
                val results = statement.executeQuery()
                LOG.info("Table with name [{}], already exists using table to store messages.", MessageQueueSettings.SQL_TABLE_NAME_DEFAULT)
            }
            catch (ex: PSQLException)
            {
                LOG.info("Failed to find table with name [{}], initialising table...", MessageQueueSettings.SQL_TABLE_NAME_DEFAULT)
                createTable()
            }
        }
    }

    private fun createTable()
    {
        val createTableQuery = sqlType.getCreateStatement()
        connection.createStatement().use { statement ->
            try
            {
                val results = statement.executeQuery(createTableQuery)
                LOG.info("Successfully created table [{}].", MessageQueueSettings.SQL_TABLE_NAME_DEFAULT)
            }
            catch (ex: PSQLException)
            {
                val errorMessage = String.format("Failed to create table with name [%s].", MessageQueueSettings.SQL_TABLE_NAME_DEFAULT)
                LOG.error(errorMessage)
                throw SqlInitialisationException(errorMessage, ex)
            }
        }
    }

    override fun getQueueForType(queueType: String): Queue<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun initialiseQueueForType(queueType: String, queue: Queue<QueueMessage>)
    {
        TODO("Not yet implemented")
    }

    override fun clearForType(queueType: String): Int
    {
        TODO("Not yet implemented")
    }

    override fun isEmptyForType(queueType: String): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun performPoll(queueType: String): Optional<QueueMessage>
    {
        TODO("Not yet implemented")
    }

    override fun keys(includeEmpty: Boolean): Set<String>
    {
        TODO("Not yet implemented")
    }

    override fun containsUUID(uuid: String): Optional<String>
    {
        TODO("Not yet implemented")
    }

    override fun performAdd(element: QueueMessage): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun performRemove(element: QueueMessage): Boolean
    {
        TODO("Not yet implemented")
    }
}
