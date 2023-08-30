package au.kilemon.messagequeue.message

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.util.SerializationUtils
import java.util.*
import javax.persistence.*

/**
 * This is used for `No-SQL` queues.
 *
 * @author github.com/Kilemonn
 */
@Document(value = QueueMessageDocument.DOCUMENT_NAME)
class QueueMessageDocument(var payload: Any?, var type: String, var assignedTo: String? = null)
{
    companion object
    {
        const val DOCUMENT_NAME: String = "multiqueuemessages"
    }

    var uuid: String = UUID.randomUUID().toString()

    @JsonIgnore
    @Id
    var id: Long? = null

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this(null, "")

    constructor(queueMessage: QueueMessage) : this()
    {
        val resolvedQueueMessage = queueMessage.resolvePayloadObject()
        this.type = resolvedQueueMessage.type
        this.uuid = resolvedQueueMessage.uuid
        this.id = resolvedQueueMessage.id
        this.payload = resolvedQueueMessage.payload
        this.assignedTo = resolvedQueueMessage.assignedTo
    }
}
