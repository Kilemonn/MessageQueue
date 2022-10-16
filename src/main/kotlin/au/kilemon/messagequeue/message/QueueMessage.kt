package au.kilemon.messagequeue.message

import java.io.Serializable
import java.util.*

/**
 * A base [QueueMessage] object which will wrap any object that is placed into the `MultiQueue`.
 * This object wraps a [Any] type `T` which is the payload to be stored in the queue. (This is actually a [Serializable] but causes issues in initialisation
 * if the type is an `interface`. This needs to be [Serializable] if you want to use it with `Redis` or anything else).
 *
 * @author github.com/KyleGonzalez
 */
class QueueMessage(val payload: Any?, val type: String, var assigned: Boolean = false, var assignedTo: String? = null): Serializable
{
    var uuid: UUID = UUID.randomUUID()

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this(null, "")

    /**
     * Retrieve a detailed [String] of the underlying object properties. If the provided [Boolean] is `true` then the payload will also be provided in the response.
     *
     * @param detailed when `true` the [payload] object will be logged as well, otherwise the [payload] will not be contained in the response
     * @return a detail [String] about this object with varying detail
     */
    fun toDetailedString(detailed: Boolean?): String
    {
        val minimalDetails = "UUID: {$uuid}, QueueType: {$type}, Is Assigned: {$assigned}, Assigned to: {$assignedTo}"
        return if (detailed == true)
        {
             "$minimalDetails, Payload: ${payload.toString()}"
        }
        else
        {
            minimalDetails
        }
    }

    override fun equals(other: Any?): Boolean
    {
        if (other == null || other !is QueueMessage)
        {
            return false
        }

        return other.uuid.toString() == this.uuid.toString()
                && this.payload == other.payload
                && this.type == other.type
    }

    override fun hashCode(): Int {
        var result = payload?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }
}
