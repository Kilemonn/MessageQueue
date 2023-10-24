package au.kilemon.messagequeue.queue.nosql.mongo.repository

import au.kilemon.messagequeue.message.QueueMessageDocument
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * A [MongoRepository] specific for [QueueMessageDocument] and queries made against them.
 * Defines additional specific queries required for interacting with [QueueMessageDocument]s.
 *
 * @author github.com/Kilemonn
 */
interface MongoQueueMessageRepository: MongoRepository<QueueMessageDocument, Long>
{
    /**
     * Get a distinct [List] of [String] [QueueMessageDocument.type] that currently exist.
     *
     * @return a [List] of all the existing [QueueMessageDocument.type] as [String]s
     */
    @Aggregation(pipeline = [ "{ '\$group': { '_id' : '\$type' } }" ])
    fun getDistinctTypes(): List<String>

    /**
     * Get a list of [QueueMessageDocument] which have [QueueMessageDocument.type] matching the provided [type].
     *
     * @param type the type to match [QueueMessageDocument.type] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.type] with the provided [type]
     */
    fun findByTypeOrderByIdAsc(type: String): List<QueueMessageDocument>

    /**
     * Find and return a [QueueMessageDocument] that matches the provided [uuid].
     *
     * @param uuid the uuid that the found [QueueMessageDocument] should match
     * @return the matching [QueueMessageDocument]
     */
    fun findByUuid(uuid: String): Optional<QueueMessageDocument>

    /**
     * Delete all [QueueMessageDocument] that have a [QueueMessageDocument.type] that matches the provided [type].
     *
     * @param type messages that are assigned this queue type will be removed
     * @return the [Int] number of deleted entries
     */
    @Modifying
    fun deleteByType(type: String): Int

    /**
     * Delete a [QueueMessageDocument] by `uuid`.
     *
     * @param uuid the UUID of the [QueueMessageDocument.uuid] to remove
     * @return the number of removed entries (most likely one since the UUID is unique)
     */
    @Modifying
    fun deleteByUuid(uuid: String): Int

    /**
     * Get the entry with the largest ID.
     *
     * @return the [QueueMessageDocument] with the largest ID, otherwise [Optional.empty]
     */
    fun findTopByOrderByIdDesc(): Optional<QueueMessageDocument>

    /**
     * Find the entity with the matching [QueueMessageDocument.type] and that has a non-null [QueueMessageDocument.assignedTo]. Sorted by ID ascending.
     *
     * @param type the type to match [QueueMessageDocument.type] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.type] with the provided [type] and non-null [QueueMessageDocument.assignedTo]
     */
    @Transactional
    fun findByTypeAndAssignedToIsNotNullOrderByIdAsc(type: String): List<QueueMessageDocument>

    /**
     * Find the entity with the matching [QueueMessageDocument.type] and [QueueMessageDocument.assignedTo]. Sorted by ID ascending.
     *
     * @param type the type to match [QueueMessageDocument.type] with
     * @param assignedTo the identifier to match [QueueMessageDocument.assignedTo] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.type] and [QueueMessageDocument.assignedTo]
     */
    @Transactional
    fun findByTypeAndAssignedToOrderByIdAsc(type: String, assignedTo: String): List<QueueMessageDocument>

    /**
     * Find the entity with the matching [QueueMessageDocument.type] and that has [QueueMessageDocument.assignedTo] set to `null`. Sorted by ID ascending.
     *
     * @param type the type to match [QueueMessageDocument.type] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.type] with the provided [type] and `null` [QueueMessageDocument.assignedTo]
     */
    @Transactional
    fun findByTypeAndAssignedToIsNullOrderByIdAsc(type: String): List<QueueMessageDocument>
}
