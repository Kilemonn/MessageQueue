package au.kilemon.messagequeue.settings

import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class MessageQueueSettings
{
    companion object
    {
        const val MESSAGE_QUEUE_TYPE: String = "MESSAGE_QUEUE_TYPE"
    }

    /**
     * Uses the `MESSSAGE_QUEUE_TYPE` environment variable, otherwise defaults to [MultiQueueType.IN_MEMORY].
     */
    @Value("#{environment.MESSAGE_QUEUE_TYPE} || 'IN_MEMORY'")
    lateinit var multiQueueType: MultiQueueType
}
