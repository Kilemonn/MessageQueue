package au.kilemon.messagequeue.queue.nosql.mongo.repository

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.message.QueueMessageDocument
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*


interface MongoQueueMessageRepository: MongoRepository<QueueMessageDocument, Long>
{
    /**
     * Find the entity which has a [QueueMessage.uuid] matching the provided [uuid].
     *
     * @param uuid the [QueueMessage.uuid] of the message to find
     * @return the [Optional] that may contain the found [QueueMessage]
     */
    @Transactional
    fun findByUuid(uuid: String): Optional<QueueMessage>

    /**
     * Delete a [QueueMessage] by `uuid`.
     *
     * @param uuid the UUID of the [QueueMessage.uuid] to remove
     * @return the number of removed entries (most likely one since the UUID is unique)
     */
    @Modifying
    @Transactional
    fun deleteByUuid(uuid: String): Int
}
