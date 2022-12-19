package au.kilemon.messagequeue.message

import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.util.*
import javax.persistence.*

/**
 * A base [QueueMessage] object which will wrap any object that is placed into the `MultiQueue`.
 * This object wraps a [Any] type `T` which is the payload to be stored in the queue. (This is actually a [Serializable] but causes issues in initialisation
 * if the type is an `interface`. This needs to be [Serializable] if you want to use it with `Redis` or anything else).
 *
 * @author github.com/KyleGonzalez
 */
@Entity
@Table(name = QueueMessage.TABLE_NAME) // TODO: Schema configuration schema = "\${${MessageQueueSettings.SQL_SCHEMA}:${MessageQueueSettings.SQL_SCHEMA_DEFAULT}}")
class QueueMessage(@Lob @Column(columnDefinition = "BLOB") val payload: Any?, @Column(nullable = false) val type: String, @Column(nullable = false) var assigned: Boolean = false, @Column(name = "assignedto") var assignedTo: String? = null): Serializable
{
    companion object
    {
        const val TABLE_NAME: String = "multiqueuemessages"
    }

    @Column(nullable = false, unique = true)
    var uuid: UUID = UUID.randomUUID()

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null

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

    /**
     * Overriding to only include specific properties when checking if messages are equal.
     * This checks the following are equal in order to return `true`:
     * - UUID
     * - payload value
     * - type
     */
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

    /**
     * Only performs a hashcode on the properties checked in [QueueMessage.equals].
     */
    override fun hashCode(): Int {
        var result = payload?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }
}
