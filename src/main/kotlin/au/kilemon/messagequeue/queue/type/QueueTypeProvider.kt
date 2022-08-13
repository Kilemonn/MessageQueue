package au.kilemon.messagequeue.queue.type

/**
 * An interface marking an object that is capable of providing a unique identifier for a specific queue.
 *
 * @author github.com/KyleGonzalez
 */
interface QueueTypeProvider
{
    /**
     * Get the unique identifier. This will be used as a "key" so
     * that services can indicate which `Queue` with the `MultiQueue` that they want to interact with.
     *
     * @return a unique identifier as a [String]
     */
    fun getIdentifier(): String
}
