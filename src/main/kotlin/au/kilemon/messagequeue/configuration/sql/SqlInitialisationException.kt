package au.kilemon.messagequeue.configuration.sql

/**
 * A custom [RuntimeException] used to indicate that there was an error during the [SqlMultiQueue] initialisation.
 *
 * @author github.com/KyleGonzalez
 */
class SqlInitialisationException(override val message: String, override val cause: Throwable? = null): RuntimeException(message, cause)
