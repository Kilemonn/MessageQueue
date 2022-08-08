package au.kilemon.messagequeue.queue.type

/**
 * Wraps a provided [String]. This is used as a unique identifier in the [MultiQueue] to determine which `MapQueue` the provided [QueueMessage] will be placed into.
 * This allows services to retrieve and search for entries in the [MultiQueue] based on their accountable [QueueType].
 *
 * @author github.com/KyleGonzalez
 */
open class QueueType(private val type: String): QueueTypeProvider
{
    /**
     * Overriding to return returns [QueueType.type].
     *
     * @return the [QueueType] identifier as a [String]
     */
    override fun getIdentifier(): String
    {
        return type
    }
}
