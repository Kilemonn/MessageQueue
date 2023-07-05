package au.kilemon.messagequeue.logging

/**
 * An `object` that holds `const` [String] values used to look up in the `MessageSource` in the `messages.properties`.
 *
 * @author github.com/Kilemonn
 */
object Messages
{
    const val INITIALISING_MESSAGE_SOURCE: String = "initialising.message.source"

    const val VERSION_START_UP: String = "version.start.up"

    const val INITIALISING_QUEUE: String = "initialising.queue"

    object RedisConfiguration
    {
        object Standalone
        {
            const val INITIALISING = "redis.configuration.standalone.initialising"

            const val NO_ENDPOINT = "redis.configuration.standalone.no.endpoint"

            const val MULTIPLE_ENDPOINTS = "redis.configuration.standalone.multiple.endpoints"
        }

        object Sentinel
        {
            const val INITIALISING = "redis.configuration.sentinel.initialising"

            const val NO_ENDPOINT = "redis.configuration.sentinel.no.endpoint"

            const val INITIALISING_WITH_HOST_AND_PORT = "redis.configuration.sentinel.initialising.with.host.and.port"
        }

    }
}
