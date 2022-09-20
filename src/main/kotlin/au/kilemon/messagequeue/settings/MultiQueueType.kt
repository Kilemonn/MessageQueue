package au.kilemon.messagequeue.settings

/**
 * An enum class used to represent the different types of underlying `MultiQueue`.
 * This will drive which implementation of the `MultiQueue` is initialised and used throughout the application.
 *
 * @author github.com/KyleGonzalez
 */
enum class MultiQueueType
{
    IN_MEMORY;
}
