package au.kilemon.messagequeue.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A marker interface used to inject a [Logger] member into the implementing class so that it has access to a logger.
 */
interface HasLogger
{
    /**
     * The class level [Logger] used to capture log events from the implementing class.
     */
    val LOG: Logger

    /**
     * Initialise and retrieve the specific [Logger] for the implementing class.
     */
    fun initialiseLogger(): Logger
    {
        return LoggerFactory.getLogger(this.javaClass.name)
    }
}
