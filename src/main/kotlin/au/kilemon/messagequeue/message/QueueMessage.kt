package au.kilemon.messagequeue.message

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.util.SerializationUtils
import java.io.Serializable
import java.util.*

/**
 * A base [QueueMessage] object which will wrap any object that is placed into the `MultiQueue`.
 * This object wraps an [Any] which is the payload to be stored in the queue. (This is actually a [Serializable] but causes issues in initialisation
 * if the type is an `interface`. This needs to be [Serializable] if you want to use it with `Redis` or anything else).
 *
 * This is used for `InMemory`, `Redis` and `SQL` queues.
 *
 * @author github.com/Kilemonn
 */
@Entity
@Table(name = QueueMessage.TABLE_NAME) // TODO: Schema configuration schema = "\${${MessageQueueSettings.SQL_SCHEMA}:${MessageQueueSettings.SQL_SCHEMA_DEFAULT}}")
class QueueMessage: Serializable
{
    companion object
    {
        const val TABLE_NAME: String = "multiqueuemessages"
    }

    @Schema(title = "Sub-queue identifier", example = "my-queue-name",
        description = "The sub-queue identifier for the sub-queue that this message is stored in.")
    @Column(name = "subqueue", nullable = false)
    var subQueue: String

    @Schema(title = "Assignee identifier", example = "owner-id",
        description = "The unique identifier of assignee who currently possessions or owns this message.")
    @Column(name = "assignedto")
    var assignedTo: String? = null

    @Schema(description = "The message payload, this can be any type of complex or simple object that you wish.")
    @Transient
    var payload: Any? = null
        set(value)
        {
            field = value
            payloadBytes = SerializationUtils.serialize(payload)
        }

    @Schema(title = "Message unique identifier", example = "7a3c1326-f038-4c17-9b6b-a9ada353f79c",
        description = "A unique identifier for this message, usually in the form of a UUID, unless created otherwise. This can be used to directly retireve or manipulate the message.")
    @Column(nullable = false, unique = true)
    var uuid: String = UUID.randomUUID().toString()

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @JsonIgnore
    // @Lob - Needed to remove to support SQLLite, seems like it was not required
    @Column(length = 50000)
    var payloadBytes: ByteArray? = SerializationUtils.serialize(payload)

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this(null, "")

    constructor(payload: Any?, subQueue: String, assignedTo: String? = null)
    {
        this.payload = payload
        this.subQueue = subQueue
        this.assignedTo = assignedTo
    }

    constructor(queueMessageDocument: QueueMessageDocument) : this()
    {
        this.subQueue = queueMessageDocument.subQueue
        this.uuid = queueMessageDocument.uuid
        this.id = queueMessageDocument.id
        this.payload = queueMessageDocument.payload
        this.assignedTo = queueMessageDocument.assignedTo
    }

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
            newMessage.subQueue = subQueue
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
     * - [uuid]
     * - [payload]
     * - [payloadBytes]
     * - [subQueue]
     */
    override fun equals(other: Any?): Boolean
    {
        if (other == null || other !is QueueMessage)
        {
            return false
        }

        return other.uuid == this.uuid
                && (this.payload == other.payload || this.payloadBytes.contentEquals(other.payloadBytes))
                && this.subQueue == other.subQueue
    }

    /**
     * Only performs a hashcode on the properties checked in [QueueMessage.equals].
     */
    override fun hashCode(): Int {
        var result = payload?.hashCode() ?: 0
        result = 31 * result + (payloadBytes?.hashCode() ?: 0)
        result = 31 * result + subQueue.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }
}
