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
        const val MULTI_QUEUE_TYPE_DEFAULT: String = "IN_MEMORY"

        const val REDIS_PREFIX: String = "REDIS_PREFIX"

        const val REDIS_ENDPOINT: String = "REDIS_ENDPOINT"
        const val REDIS_ENPOINT_DEFAULT: String = "127.0.0.1"

        const val REDIS_PORT: String = "REDIS_PORT"
        const val REDIS_PORT_DEFAULT = "6379"

        // Redis sentinel related properties
        const val REDIS_USE_SENTINELS = "REDIS_USE_SENTINELS"

        const val REDIS_MASTER_NAME = "REDIS_MASTER_NAME"
        const val REDIS_MASTER_NAME_DEFAULT = "mymaster"
    }

    /**
     * Uses the [MULTI_QUEUE_TYPE] environment variable, otherwise defaults to [MultiQueueType.IN_MEMORY].
     */
    @Value("#{environment.$MULTI_QUEUE_TYPE} || '$MULTI_QUEUE_TYPE_DEFAULT'")
    lateinit var multiQueueType: MultiQueueType

    /**
     * Uses the [REDIS_PREFIX] to set a prefix used for all redis entry keys.
     */
    @Value("#{environment.$REDIS_PREFIX} || ''")
    lateinit var redisPrefix: String

    @Value("#{environment.$REDIS_ENDPOINT} || '$REDIS_ENPOINT_DEFAULT'")
    lateinit var redisEndpoint: String

    @Value("#{environment.$REDIS_PORT} || '$REDIS_PORT_DEFAULT'")
    lateinit var redisPort: String

    @Value("#{environment.$REDIS_USE_SENTINELS} || 'false'")
    lateinit var redisUseSentinels: String

    @Value("#{environment.$REDIS_MASTER_NAME} || '$REDIS_MASTER_NAME_DEFAULT'")
    lateinit var redisMasterName: String
}
