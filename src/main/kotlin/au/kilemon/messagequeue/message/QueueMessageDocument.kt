package au.kilemon.messagequeue.message

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * This is used for `No-SQL` queues.
 *
 * @author github.com/Kilemonn
 */
@Document(value = QueueMessageDocument.DOCUMENT_NAME)
class QueueMessageDocument(var payload: Any?, var subQueue: String, var assignedTo: String? = null)
{
    companion object
    {
        const val DOCUMENT_NAME: String = "multiqueuemessages"
    }

    @Id
    @OptIn(ExperimentalUuidApi::class)
    var uuid: String = Uuid.generateV7().toString()

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this(null, "")

    constructor(queueMessage: QueueMessage) : this()
    {
        val resolvedQueueMessage = queueMessage.resolvePayloadObject()
        this.subQueue = resolvedQueueMessage.subQueue
        this.uuid = resolvedQueueMessage.uuid
        this.payload = resolvedQueueMessage.payload
        this.assignedTo = resolvedQueueMessage.assignedTo
    }
}
