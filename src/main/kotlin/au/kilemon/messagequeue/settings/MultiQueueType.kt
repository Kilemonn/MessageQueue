package au.kilemon.messagequeue.settings

/**
 * An enum class used to represent the different types of underlying `MultiQueue`.
 * This will drive which implementation of the `MultiQueue` is initialised and used throughout the application.
 *
 * @author github.com/KyleGonzalez
 */
enum class MultiQueueType
{
    /**
     * Will initialise an in-memory multiqueue to store queue messages.
     */
    IN_MEMORY,

    /**
     * Will connect to the defined redis service to store queue messages.
     */
    REDIS,

    /**
     * Will initialise and connect to a defined SQL database instance to store queue messages against.
     */
    SQL;
}
