package au.kilemon.messagequeue.queue.sql.repository

import au.kilemon.messagequeue.message.QueueMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * A [JpaRepository] specific for [QueueMessage] and queries made against them.
 * Defines additional specific queries required for interacting with [QueueMessage]s.
 *
 * Reference: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation
 *
 * @author github.com/Kilemonn
 */
@Repository
interface SqlQueueMessageRepository: JpaRepository<QueueMessage, Long>
{
    /**
     * Delete a [QueueMessage] by the provided [QueueMessage.subQueue] [String].
     *
     * @param subQueue the [QueueMessage.subQueue] to remove entries by
     * @return the number of deleted entities
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM QueueMessage WHERE subQueue = ?1")
    fun deleteBySubQueue(subQueue: String): Int

    /**
     * Get a distinct [List] of [String] [QueueMessage.subQueue] that currently exist.
     *
     * @return a [List] of all the existing [QueueMessage.subQueue] as [String]s
     */
    @Transactional
    @Query("SELECT DISTINCT subQueue FROM QueueMessage")
    fun findDistinctSubQueue(): List<String>

    /**
     * Get a list of [QueueMessage] which have [QueueMessage.subQueue] matching the provided [subQueue].
     *
     * @param subQueue to match [QueueMessage.subQueue] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.subQueue] with the provided [subQueue]
     */
    @Transactional
    fun findBySubQueueOrderByIdAsc(subQueue: String): List<QueueMessage>

    /**
     * Find the entity with the matching [QueueMessage.subQueue] and that has a non-null [QueueMessage.assignedTo]. Sorted by ID ascending.
     *
     * @param subQueue to match [QueueMessage.subQueue] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.subQueue] with the provided [subQueue] and non-null [QueueMessage.assignedTo]
     */
    @Transactional
    fun findBySubQueueAndAssignedToIsNotNullOrderByIdAsc(subQueue: String): List<QueueMessage>

    /**
     * Find the entity with the matching [QueueMessage.subQueue] and that has [QueueMessage.assignedTo] set to `null`. Sorted by ID ascending.
     *
     * @param subQueue the type to match [QueueMessage.subQueue] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.subQueue] with the provided [subQueue] and `null` [QueueMessage.assignedTo]
     */
    @Transactional
    fun findBySubQueueAndAssignedToIsNullOrderByIdAsc(subQueue: String): List<QueueMessage>

    /**
     * Find the entity with the matching [QueueMessage.subQueue] and [QueueMessage.assignedTo]. Sorted by ID ascending.
     *
     * @param subQueue the type to match [QueueMessage.subQueue] with
     * @param assignedTo the identifier to match [QueueMessage.assignedTo] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.subQueue] and [QueueMessage.assignedTo]
     */
    @Transactional
    fun findBySubQueueAndAssignedToOrderByIdAsc(subQueue: String, assignedTo: String): List<QueueMessage>

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
