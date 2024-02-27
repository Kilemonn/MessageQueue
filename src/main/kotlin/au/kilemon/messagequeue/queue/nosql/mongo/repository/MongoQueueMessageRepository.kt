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
     * Get a distinct [List] of [String] [QueueMessageDocument.subQueue] that currently exist.
     *
     * @return a [List] of all the existing [QueueMessageDocument.subQueue] as [String]s
     */
    @Aggregation(pipeline = [ "{ '\$group': { '_id' : '\$subQueue' } }" ])
    fun getDistinctSubQueues(): List<String>

    /**
     * Get a list of [QueueMessageDocument] which have [QueueMessageDocument.subQueue] matching the provided [subQueue].
     *
     * @param subQueue the type to match [QueueMessageDocument.subQueue] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.subQueue] with the provided [subQueue]
     */
    fun findBySubQueueOrderByIdAsc(subQueue: String): List<QueueMessageDocument>

    /**
     * Find and return a [QueueMessageDocument] that matches the provided [uuid].
     *
     * @param uuid the uuid that the found [QueueMessageDocument] should match
     * @return the matching [QueueMessageDocument]
     */
    fun findByUuid(uuid: String): Optional<QueueMessageDocument>

    /**
     * Delete all [QueueMessageDocument] that have a [QueueMessageDocument.subQueue] that matches the provided [subQueue].
     *
     * @param subQueue messages that are assigned this sub-queue will be removed
     * @return the [Int] number of deleted entries
     */
    @Modifying
    fun deleteBySubQueue(subQueue: String): Int

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
     * Find the entity with the matching [QueueMessageDocument.subQueue] and that has a non-null [QueueMessageDocument.assignedTo]. Sorted by ID ascending.
     *
     * @param subQueue the type to match [QueueMessageDocument.subQueue] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.subQueue] with the provided [subQueue] and non-null [QueueMessageDocument.assignedTo]
     */
    @Transactional
    fun findBySubQueueAndAssignedToIsNotNullOrderByIdAsc(subQueue: String): List<QueueMessageDocument>

    /**
     * Find the entity with the matching [QueueMessageDocument.subQueue] and [QueueMessageDocument.assignedTo]. Sorted by ID ascending.
     *
     * @param subQueue the type to match [QueueMessageDocument.subQueue] with
     * @param assignedTo the identifier to match [QueueMessageDocument.assignedTo] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.subQueue] and [QueueMessageDocument.assignedTo]
     */
    @Transactional
    fun findBySubQueueAndAssignedToOrderByIdAsc(subQueue: String, assignedTo: String): List<QueueMessageDocument>

    /**
     * Find the entity with the matching [QueueMessageDocument.subQueue] and that has [QueueMessageDocument.assignedTo] set to `null`. Sorted by ID ascending.
     *
     * @param subQueue the type to match [QueueMessageDocument.subQueue] with
     * @return a [List] of [QueueMessageDocument] who have a matching [QueueMessageDocument.subQueue] with the provided [subQueue] and `null` [QueueMessageDocument.assignedTo]
     */
    @Transactional
    fun findBySubQueueAndAssignedToIsNullOrderByIdAsc(subQueue: String): List<QueueMessageDocument>
}
