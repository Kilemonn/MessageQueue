package au.kilemon.messagequeue.configuration.nosql.mongo

import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A class that creates the required [Bean] objects when mongo is enabled.
 *
 * @author github.com/Kilemonn
 */
@Configuration
class MongoConfiguration
{
    @Autowired
    private lateinit var messageQueueSettings: MessageQueueSettings

    @Bean
    @ConditionalOnProperty(name=[MessageQueueSettings.STORAGE_MEDIUM], havingValue="MONGO")
    fun getMongoClients(): MongoClient
    {
        if (messageQueueSettings.mongoUri.isNotBlank())
        {
            return MongoClients.create(messageQueueSettings.mongoUri)
        }
        else
        {
            var credentials = ""
            if (messageQueueSettings.mongoUsername.isNotBlank() || messageQueueSettings.mongoPassword.isNotBlank())
            {
                credentials = "${messageQueueSettings.mongoUsername.isNotBlank()}:${messageQueueSettings.mongoPassword}@"
            }

            val endpoint = "mongodb://$credentials${messageQueueSettings.mongoHost}:${messageQueueSettings.mongoPort}/${messageQueueSettings.mongoDatabase}"
            return MongoClients.create(endpoint)
        }
    }
}
