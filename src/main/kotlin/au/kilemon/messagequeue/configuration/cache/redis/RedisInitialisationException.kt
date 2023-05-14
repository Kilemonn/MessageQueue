package au.kilemon.messagequeue.configuration.cache.redis

import lombok.Generated

/**
 * A custom [RuntimeException] used to indicate that there was an error during the [RedisConfiguration] initialisation.
 *
 * @author github.com/Kilemonn
 */
class RedisInitialisationException(@get:Generated override val message: String, @get:Generated override val cause: Throwable? = null): RuntimeException(message, cause)
