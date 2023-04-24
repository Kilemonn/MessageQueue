package au.kilemon.messagequeue.message

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.util.SerializationUtils
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
class QueueMessage(payload: Any?, @Column(nullable = false) var type: String, @Column(name = "assignedto") var assignedTo: String? = null): Serializable
{
    companion object
    {
        const val TABLE_NAME: String = "multiqueuemessages"
    }

    @Transient
    var payload = payload
        set(value)
        {
            field = value
            payloadBytes = SerializationUtils.serialize(payload)
        }

    @Column(nullable = false, unique = true)
    var uuid: String = UUID.randomUUID().toString()

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @JsonIgnore
    @Lob
    @Column
    var payloadBytes: ByteArray? = SerializationUtils.serialize(payload)

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this(null, "")

    /**
     * When the [QueueMessage] is read back from a database serialised form, only the
     * [QueueMessage.payloadBytes] will be persisted, [QueueMessage.payload] will still be `null` by default.
     *
     * This method is used to initialise the [QueueMessage.payload] property after the [QueueMessage] is read from the database
     * so that when it is provided as a JSON response to the caller that it has the [QueueMessage.payload] property set correctly.
     *
     * This will only alter the underlying object if the [QueueMessage.payloadBytes] is `not null` and [QueueMessage.payload] is `null`.
     *
     * This should only be called by `SqlMultiQueue` implementations so far. It should be specialised and not called by the framework.
     *
     * @return the current instance with the conditionally modified [QueueMessage.payload] member based on the points above
     */
    fun resolvePayloadObject(): QueueMessage
    {
        if (payloadBytes != null && payload == null)
        {
            payload = SerializationUtils.deserialize(payloadBytes)
        }
        return this
    }

    /**
     * If the provided [Boolean] is `true` then the payload will also be provided in the response.
     * Otherwise, the payload should be removed in the response object.
     *
     * @param detailed when `true` the [payload] object will be logged as well, otherwise the [payload] will not be contained in the response or `null`.
     * @return [QueueMessage] that is either a copy of `this` without the payload, or `this` with a resolved payload
     */
    fun removePayload(detailed: Boolean?): QueueMessage
    {
        if (detailed == false)
        {
            // Create a temporary object since the object is edited in place if we are using the in-memory queue
            val newMessage = QueueMessage()
            newMessage.type = type
            newMessage.assignedTo = assignedTo
            newMessage.uuid = uuid
            newMessage.payload = "***" // Mark as stars to indicate that it is there but not returned
            return newMessage
        }
        resolvePayloadObject()
        return this
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

        return other.uuid == this.uuid
                && (this.payload == other.payload || this.payloadBytes.contentEquals(other.payloadBytes))
                && this.type == other.type
    }

    /**
     * Only performs a hashcode on the properties checked in [QueueMessage.equals].
     */
    override fun hashCode(): Int {
        var result = payload?.hashCode() ?: 0
        result = 31 * result + (payloadBytes?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }
}
