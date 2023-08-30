package au.kilemon.messagequeue.settings

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import lombok.Generated
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
 * When `SQL` is used, the following property must be provided:
 * `spring.jpa.hibernate.ddl-auto=create`
 * This will ensure the underlying tables will be created on start up if they do not exist.
 *
 * @author github.com/Kilemonn
 */
@Component
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
        const val SQL_ENDPOINT: String = "spring.datasource.url"
        const val SQL_USERNAME: String = "spring.datasource.username"
        const val SQL_PASSWORD: String = "spring.datasource.password"

        /**
         * Start MONGO related properties
         */
        const val MONGO_HOST: String = "spring.data.mongodb.host"
        const val MONGO_DATABASE: String = "spring.data.mongodb.database"
        const val MONGO_PORT: String = "spring.data.mongodb.port"
        const val MONGO_USERNAME: String = "spring.data.mongodb.username"
        const val MONGO_PASSWORD: String = "spring.data.mongodb.password"
        const val MONGO_URI: String = "spring.data.mongodb.uri"

        /**
         * SQL Schema properties
         */
        const val SQL_SCHEMA: String = "SQL_SCHEMA"
        const val SQL_SCHEMA_DEFAULT: String = "public"
    }

    /**
     * `Optional` uses the [MULTI_QUEUE_TYPE] environment variable to determine where
     * the underlying multi queue is persisted. It can be any value of [MultiQueueType].
     * Defaults to [MultiQueueType.IN_MEMORY] ([MULTI_QUEUE_TYPE_DEFAULT]).
     */
    @SerializedName(MULTI_QUEUE_TYPE)
    @JsonProperty(MULTI_QUEUE_TYPE)
    @Value("\${$MULTI_QUEUE_TYPE:$MULTI_QUEUE_TYPE_DEFAULT}")
    @get:Generated
    @set:Generated
    lateinit var multiQueueType: String

    /**
     * `Optional` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * Uses the [REDIS_PREFIX] to set a prefix used for all redis entry keys.
     *
     * E.g. if the initial value for the redis entry is "my-key" and no prefix is defined the entries would be stored under "my-key".
     * Using the same scenario if the prefix is "prefix" then the resultant key would be "prefixmy-key".
     */
    @SerializedName(REDIS_PREFIX)
    @JsonProperty(REDIS_PREFIX)
    @Value("\${$REDIS_PREFIX:}")
    @get:Generated
    @set:Generated
    lateinit var redisPrefix: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * The input endpoint string which is used for both standalone and the sentinel redis configurations.
     * This supports a comma separated list or single definition of a redis endpoint in the following formats:
     * `<endpoint>:<port>,<endpoint2>:<port2>,<endpoint3>`
     *
     * If not provided [REDIS_ENDPOINT_DEFAULT] will be used by default.
     */
    @SerializedName(REDIS_ENDPOINT)
    @JsonProperty(REDIS_ENDPOINT)
    @Value("\${$REDIS_ENDPOINT:$REDIS_ENDPOINT_DEFAULT}")
    @get:Generated
    @set:Generated
    lateinit var redisEndpoint: String

    /**
     * `Optional` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * Indicates whether the `MultiQueue` should connect directly to the redis instance or connect via one or more sentinel instances.
     * If set to `true` the `MultiQueue` will create a sentinel pool connection instead of a direct connection which is what would occur if this is left as `false`.
     * By default, this is `false`.
     */
    @SerializedName(REDIS_USE_SENTINELS)
    @JsonProperty(REDIS_USE_SENTINELS)
    @Value("\${$REDIS_USE_SENTINELS:false}")
    @get:Generated
    @set:Generated
    lateinit var redisUseSentinels: String

    /**
     * `Optional` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.REDIS].
     * `Required` when [redisUseSentinels] is set to `true`. Is used to indicate the name of the redis master instance.
     * By default, this is [REDIS_MASTER_NAME_DEFAULT].
     */
    @SerializedName(REDIS_MASTER_NAME)
    @JsonProperty(REDIS_MASTER_NAME)
    @Value("\${$REDIS_MASTER_NAME:$REDIS_MASTER_NAME_DEFAULT}")
    @get:Generated
    @set:Generated
    lateinit var redisMasterName: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     * This defines the database connection string e.g:
     * `"jdbc:mysql://localhost:3306/message-queue"`
     */
    @SerializedName(SQL_ENDPOINT)
    @JsonProperty(SQL_ENDPOINT)
    @Value("\${$SQL_ENDPOINT:}")
    @get:Generated
    @set:Generated
    lateinit var sqlEndpoint: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     * This is the username/account name used to access the database defined in [SQL_ENDPOINT].
     */
    @SerializedName(SQL_USERNAME)
    @JsonProperty(SQL_USERNAME)
    @Value("\${$SQL_USERNAME:}")
    @get:Generated
    @set:Generated
    lateinit var sqlUsername: String

    /**
     * `Required` when [MULTI_QUEUE_TYPE] is set to [MultiQueueType.SQL].
     * This is the password used to access the database defined in [SQL_ENDPOINT].
     */
    // TODO: Commenting out since it is unused and returned in the settings endpoint without masking
    // @JsonIgnore
    // @SerializedName(SQL_PASSWORD)
    // @JsonProperty(SQL_PASSWORD)
    // @Value("\${$SQL_PASSWORD:}")
    // lateinit var sqlPassword: String

    /**
     * Required when [MultiQueueType.MONGO] is used and [mongoUri] is empty.
     * It specifies the host name that the mongo db is available at.
     */
    @SerializedName(MONGO_HOST)
    @JsonProperty(MONGO_HOST)
    @Value("\${$MONGO_HOST:}")
    @get:Generated
    @set:Generated
    lateinit var mongoHost: String

    /**
     * Required when [MultiQueueType.MONGO] is used and [mongoUri] is empty.
     * It specifies the port that the mongo db is available on.
     */
    @SerializedName(MONGO_PORT)
    @JsonProperty(MONGO_PORT)
    @Value("\${$MONGO_PORT:}")
    @get:Generated
    @set:Generated
    lateinit var mongoPort: String

    /**
     * Required when [MultiQueueType.MONGO] is used and [mongoUri] is empty.
     * It specifies the database you wish to connect to.
     */
    @SerializedName(MONGO_DATABASE)
    @JsonProperty(MONGO_DATABASE)
    @Value("\${$MONGO_DATABASE:}")
    @get:Generated
    @set:Generated
    lateinit var mongoDatabase: String

    /**
     * Required when [MultiQueueType.MONGO] is used and [mongoUri] is empty.
     * It specifies the username that you wish to connect with.
     */
    @SerializedName(MONGO_USERNAME)
    @JsonProperty(MONGO_USERNAME)
    @Value("\${$MONGO_USERNAME:}")
    @get:Generated
    @set:Generated
    lateinit var mongoUsername: String

    /**
     * Required when [MultiQueueType.MONGO] is used and [mongoUri] is empty.
     * It specifies the password for the user that you wish to connect with.
     */
    // TODO: Commenting out since it is unused and returned in the settings endpoint without masking
    // @JsonIgnore
    // @SerializedName(MONGO_PASSWORD)
    // @JsonProperty(MONGO_PASSWORD)
    // @Value("\${MONGO_PASSWORD:}")
    // lateinit var mongoPassword: String

    /**
     * Required when [MultiQueueType.MONGO] is used and the above mongo properties are empty.
     * It specifies all properties of the mongo connection in the format of `mongodb://<username>:<password>@<host>:<port>/<database>`.
     */
    @SerializedName(MONGO_URI)
    @JsonProperty(MONGO_URI)
    @Value("\${$MONGO_URI:}")
    @get:Generated
    @set:Generated
    lateinit var mongoUri: String
}
