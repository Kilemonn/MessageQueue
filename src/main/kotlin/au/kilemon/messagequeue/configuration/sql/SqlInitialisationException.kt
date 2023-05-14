package au.kilemon.messagequeue.configuration.sql

import lombok.Generated

/**
 * A custom [RuntimeException] used to indicate that there was an error during the [SqlMultiQueue] initialisation.
 *
 * @author github.com/Kilemonn
 */
class SqlInitialisationException(@get:Generated override val message: String, @get:Generated override val cause: Throwable? = null): RuntimeException(message, cause)
