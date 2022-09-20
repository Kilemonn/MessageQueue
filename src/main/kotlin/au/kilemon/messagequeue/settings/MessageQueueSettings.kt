package au.kilemon.messagequeue.settings

import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * An object that holds application level properties and is set initially on application start up.
 * This will control things such as:
 * - Credentials to external data storage
 * - The type of `MultiQueue` being used
 * - Other utility configuration for the application to use.
 *
 * @author github.com/KyleGonzalez
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class MessageQueueSettings
{
    companion object
    {
        const val MULTI_QUEUE_TYPE: String = "MULTI_QUEUE_TYPE"
    }

    /**
     * Uses the [MULTI_QUEUE_TYPE] environment variable, otherwise defaults to [MultiQueueType.IN_MEMORY].
     */
    @Value("#{environment.MULTI_QUEUE_TYPE} || 'IN_MEMORY'")
    lateinit var multiQueueType: MultiQueueType
}
