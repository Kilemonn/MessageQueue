package au.kilemon.messagequeue.message

import lombok.EqualsAndHashCode
import java.io.Serializable
import java.util.UUID

/**
 * A base [QueueMessage] object which will wrap any object that is placed into the `MultiQueue`.
 * This object wraps a [Serializable] type `T` which is the payload to be stored in the queue.
 *
 * @author github.com/KyleGonzalez
 */
@EqualsAndHashCode
data class QueueMessage(val payload: Serializable?, val type: String, @EqualsAndHashCode.Exclude var isConsumed: Boolean = false, @EqualsAndHashCode.Exclude var consumedBy: String? = null): Serializable
{
    private val uuid: UUID = UUID.randomUUID()

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this(null, "")

    /**
     * Retrieve a detailed [String] of the underlying object properties. If the provided [Boolean] is `true` then the payload will also be provided in the response.
     *
     * @param detailed when `true` the [#payload] object will be logged as well, otherwise the [#payload] will not be contained in the response
     * @return a detail [String] about this object with varying detail
     */
    fun toDetailedString(detailed: Boolean?): String
    {
        val minimalDetails = "UUID: {$uuid}, QueueType: {$type}, Is Consumed: {$isConsumed}, Consumed by: {$consumedBy}"
        return if (detailed == true)
        {
             "$minimalDetails, Payload: ${payload.toString()}"
        }
        else
        {
            minimalDetails
        }
    }
}
