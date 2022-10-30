package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.settings.MessageQueueSettings
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
        val statement = connection.createStatement()
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
