package au.kilemon.messagequeue.queue.repository

import au.kilemon.messagequeue.message.QueueMessage
import java.util.*

/**
 * A collection of Repository method specific for [QueueMessage] and queries made against them.
 *
 * Reference: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation
 *
 * @author github.com/Kilemonn
 */
interface QueueMessageRepository
{
    /**
     * Delete a [QueueMessage] by the provided [QueueMessage.type] [String].
     *
     * @param type the [QueueMessage.type] to remove entries by
     * @return the number of deleted entities
     */
    fun deleteByType(type: String): Int

    /**
     * Get a distinct [List] of [String] [QueueMessage.type] that currently exist.
     *
     * @return a [List] of all the existing [QueueMessage.type] as [String]s
     */
    fun findDistinctType(): List<String>

    /**
     * Get a list of [QueueMessage] which have [QueueMessage.type] matching the provided [type].
     *
     * @param type the type to match [QueueMessage.type] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.type] with the provided [type]
     */
    fun findByTypeOrderByIdAsc(type: String): List<QueueMessage>

    /**
     * Find the entity with the matching [QueueMessage.type] and that has a non-null [QueueMessage.assignedTo]. Sorted by ID ascending.
     *
     * @param type the type to match [QueueMessage.type] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.type] with the provided [type] and non-null [QueueMessage.assignedTo]
     */
    fun findByTypeAndAssignedToIsNotNullOrderByIdAsc(type: String): List<QueueMessage>

    /**
     * Find the entity with the matching [QueueMessage.type] and that has [QueueMessage.assignedTo] set to `null`. Sorted by ID ascending.
     *
     * @param type the type to match [QueueMessage.type] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.type] with the provided [type] and `null` [QueueMessage.assignedTo]
     */
    fun findByTypeAndAssignedToIsNullOrderByIdAsc(type: String): List<QueueMessage>

    /**
     * Find the entity with the matching [QueueMessage.type] and [QueueMessage.assignedTo]. Sorted by ID ascending.
     *
     * @param type the type to match [QueueMessage.type] with
     * @param assignedTo the identifier to match [QueueMessage.assignedTo] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.type] and [QueueMessage.assignedTo]
     */
    fun findByTypeAndAssignedToOrderByIdAsc(type: String, assignedTo: String): List<QueueMessage>

    /**
     * Find the entity which has a [QueueMessage.uuid] matching the provided [uuid].
     *
     * @param uuid the [QueueMessage.uuid] of the message to find
     * @return the [Optional] that may contain the found [QueueMessage]
     */
    fun findByUuid(uuid: String): Optional<QueueMessage>

    /**
     * Delete a [QueueMessage] by `uuid`.
     *
     * @param uuid the UUID of the [QueueMessage.uuid] to remove
     * @return the number of removed entries (most likely one since the UUID is unique)
     */
    fun deleteByUuid(uuid: String): Int
}
