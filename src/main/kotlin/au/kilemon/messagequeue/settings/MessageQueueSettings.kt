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
        const val REDIS_ENDPOINT_DEFAULT: String = "127.0.0.1"

        // Redis sentinel related properties
        const val REDIS_USE_SENTINELS = "REDIS_USE_SENTINELS"

        const val REDIS_MASTER_NAME = "REDIS_MASTER_NAME"
        const val REDIS_MASTER_NAME_DEFAULT = "mymaster"
    }

    /**
     * Uses the [MULTI_QUEUE_TYPE] environment variable, otherwise defaults to [MultiQueueType.IN_MEMORY] ([MULTI_QUEUE_TYPE_DEFAULT]).
     */
    @Value("\${$MULTI_QUEUE_TYPE:$MULTI_QUEUE_TYPE_DEFAULT}")
    lateinit var multiQueueType: String

    /**
     * Uses the [REDIS_PREFIX] to set a prefix used for all redis entry keys.
     *
     * E.g. if the initial value for the redis entry is "my-key" and no prefix is defined the entries would be stored under "my-key".
     * Using the same scenario if the prefix is "prefix" then the resultant key would be "prefixmy-key".
     */
    @Value("\${$REDIS_PREFIX:}")
    lateinit var redisPrefix: String

    /**
     * The input endpoint string which is used for both standalone and the sentinel redis configurations.
     * This supports a comma separated list or single definition of a redis endpoint in the following formats:
     * `<endpoint>:<port>,<endpoint2>:<port2>,<endpoint3>`
     *
     * If not provided [REDIS_ENDPOINT_DEFAULT] will be used by default.
     */
    @Value("\${$REDIS_ENDPOINT:$REDIS_ENDPOINT_DEFAULT}")
    lateinit var redisEndpoint: String

    /**
     * Indicates whether the `MultiQueue` should connect directly to the redis instance or connect via one or more sentinel instances.
     * If set to `true` the `MultiQueue` will create a sentinel pool connection instead of a direct connection which is what would occur if this is left as `false`.
     * By default, this is `false`.
     */
    @Value("\${$REDIS_USE_SENTINELS:false}")
    lateinit var redisUseSentinels: String

    /**
     * Required when [redisUseSentinels] is set to `true`. Is used to indicate the name of the redis master instance.
     * By default, this is [REDIS_MASTER_NAME_DEFAULT].
     */
    @Value("\${$REDIS_MASTER_NAME:$REDIS_MASTER_NAME_DEFAULT}")
    lateinit var redisMasterName: String
}
