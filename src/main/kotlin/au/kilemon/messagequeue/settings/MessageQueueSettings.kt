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

        /**
         * Start redis related properties
         */
        const val REDIS_PREFIX: String = "REDIS_PREFIX"

        const val REDIS_ENDPOINT: String = "REDIS_ENDPOINT"
        const val REDIS_ENDPOINT_DEFAULT: String = "127.0.0.1"

        // Redis sentinel related properties
        const val REDIS_USE_SENTINELS: String = "REDIS_USE_SENTINELS"

        const val REDIS_MASTER_NAME: String = "REDIS_MASTER_NAME"
        const val REDIS_MASTER_NAME_DEFAULT: String = "mymaster"

        /**
         * Start SQL related properties
         */
        const val SQL_DRIVER: String = "SQL_DRIVER"
        const val SQL_DIALECT: String = "SQL_DIALECT"
        const val SQL_ENDPOINT: String = "SQL_ENDPOINT"
        const val SQL_USERNAME: String = "SQL_USERNAME"
        const val SQL_PASSWORD: String = "SQL_PASSWORD"
    }

    /**
     * `Optional` uses the [MULTI_QUEUE_TYPE] environment variable to determine where
     * the underlying multi queue is persisted. It can be any value of [MultiQueueType].
     * Defaults to [MultiQueueType.IN_MEMORY] ([MULTI_QUEUE_TYPE_DEFAULT]).
     */
    @Value("\${$MULTI_QUEUE_TYPE:$MULTI_QUEUE_TYPE_DEFAULT}")
    lateinit var multiQueueType: String

    /**
     * `Optional` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * Uses the [REDIS_PREFIX] to set a prefix used for all redis entry keys.
     *
     * E.g. if the initial value for the redis entry is "my-key" and no prefix is defined the entries would be stored under "my-key".
     * Using the same scenario if the prefix is "prefix" then the resultant key would be "prefixmy-key".
     */
    @Value("\${$REDIS_PREFIX:}")
    lateinit var redisPrefix: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * The input endpoint string which is used for both standalone and the sentinel redis configurations.
     * This supports a comma separated list or single definition of a redis endpoint in the following formats:
     * `<endpoint>:<port>,<endpoint2>:<port2>,<endpoint3>`
     *
     * If not provided [REDIS_ENDPOINT_DEFAULT] will be used by default.
     */
    @Value("\${$REDIS_ENDPOINT:$REDIS_ENDPOINT_DEFAULT}")
    lateinit var redisEndpoint: String

    /**
     * `Optional` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * Indicates whether the `MultiQueue` should connect directly to the redis instance or connect via one or more sentinel instances.
     * If set to `true` the `MultiQueue` will create a sentinel pool connection instead of a direct connection which is what would occur if this is left as `false`.
     * By default, this is `false`.
     */
    @Value("\${$REDIS_USE_SENTINELS:false}")
    lateinit var redisUseSentinels: String

    /**
     * `Optional` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * `Required` when [redisUseSentinels] is set to `true`. Is used to indicate the name of the redis master instance.
     * By default, this is [REDIS_MASTER_NAME_DEFAULT].
     */
    @Value("\${$REDIS_MASTER_NAME:$REDIS_MASTER_NAME_DEFAULT}")
    lateinit var redisMasterName: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     *  Defines the underlying driver which is used to connect to the requested database.
     *
     *  Currently supports:
     *  - `com.mysql.jdbc.Driver`
     *  - `org.postgresql.Driver`
     *  - `oracle.jdbc.driver.OracleDriver`
     */
    @Value("\${$SQL_DRIVER:}")
    lateinit var sqlDriver: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     * This defines the database connection string e.g:
     * `"jdbc:mysql://localhost:3306/message-queue"`
     */
    @Value("\${$SQL_ENDPOINT:}")
    lateinit var sqlEndpoint: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     * This is the username/account name used to access the database defined in [SQL_ENDPOINT].
     */
    @Value("\${$SQL_USERNAME:}")
    lateinit var sqlUsername: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     * This is the password used to access the database defined in [SQL_ENDPOINT].
     */
    @Value("\${$SQL_PASSWORD:}")
    lateinit var sqlPassword: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     * This is the dialect that hibernate will use when interacting with the underlying database.
     * E.g.
     * - `org.hibernate.dialect.MySQLDialect`
     * - `org.hibernate.dialect.PostgreSQLDialect`
     * - `org.hibernate.dialect.OracleDialect`
     */
    @Value("\${$SQL_DIALECT:}")
    lateinit var sqlDialect: String
}
