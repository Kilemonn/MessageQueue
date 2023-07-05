package au.kilemon.messagequeue.logging

/**
 * An `object` that holds `const` [String] values used to look up in the `MessageSource` in the `exception_messages.properties`.
 *
 * @author github.com/Kilemonn
 */
object ExceptionMessages
{
    object RedisConfiguration
    {
        object Standalone
        {
            const val NO_ENDPOINT = "redis.standalone.exception.no.endpoints"
        }

        object Sentinel
        {
            const val NO_ENDPOINT = "redis.sentinel.exception.no.endpoints"
        }
    }
}
