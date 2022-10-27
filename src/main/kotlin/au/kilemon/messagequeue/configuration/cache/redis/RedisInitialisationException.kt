package au.kilemon.messagequeue.configuration.cache.redis

/**
 * A custom [RuntimeException] used to indicate that there was an error during the [RedisConfiguration] initialisation.
 *
 * @author github.com/KyleGonzalez
 */
class RedisInitialisationException(override val message: String, override val cause: Throwable? = null): RuntimeException(message, cause)
