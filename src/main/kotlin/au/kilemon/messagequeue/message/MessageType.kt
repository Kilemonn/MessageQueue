package au.kilemon.messagequeue.message

/**
 * Wraps a provided [String]. This is used as a unique identifier in the [MultiQueue] to determine which `Queue` the provided [Message] will be placed into.
 * This allows services to retrieve and search for entries in the [MultiQueue] based on their accountable [MessageType].
 *
 * @author github.com/KyleGonzalez
 */
open class MessageType(private val type: String)
{
    /**
     * Get the unique [MessageType] identifier. This will be used as a "key" so
     * that services can indicate which [MessageType]s they want to interact with.
     *
     * By default, this returns [MessageType.type].
     *
     * @return the [MessageType] identifier as a [String]
     */
    open fun getIdentifier(): String
    {
        return type
    }
}
