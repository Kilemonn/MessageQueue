package au.kilemon.messagequeue.queue.sql.repository

import au.kilemon.messagequeue.message.QueueMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * A [JpaRepository] specific for [QueueMessage] and queries made against them.
 * Defines additional specific queries required for interacting with [QueueMessage]s.
 *
 * @author github.com/KyleGonzalez
 */
@Repository
interface QueueMessageRepository: JpaRepository<QueueMessage, Int>
{
    /**
     * Delete a [QueueMessage] by the provided [QueueMessage.type] [String].
     *
     * @param type the [QueueMessage.type] to remove entries by
     * @return the number of deleted entities
     */
    @Modifying
    @Query("DELETE FROM QueueMessage WHERE type = ?1")
    fun deleteByType(type: String): Int

    /**
     * Get a distinct [List] of [String] [QueueMessage.type] that currently exist.
     *
     * @return a [List] of all the existing [QueueMessage.type] as [String]s
     */
    @Query("SELECT DISTINCT type FROM QueueMessage")
    fun findDistinctType(): List<String>

    /**
     * Get a list of [QueueMessage] which have [QueueMessage.type] matching the provided [type].
     *
     * @param type the type to match [QueueMessage.type] with
     * @return a [List] of [QueueMessage] who have a matching [QueueMessage.type] with the provided [type]
     */
    fun findByTypeOrderByIdAsc(type: String): List<QueueMessage>

    /**
     * Find the entity which has a [QueueMessage.uuid] matching the provided [uuid].
     *
     * @param uuid the [QueueMessage.uuid] of the message to find
     * @return the [Optional] that may contain the found [QueueMessage]
     */
    fun findByUuid(uuid: String): Optional<QueueMessage>

    @Modifying
    fun deleteByUuid(uuid: String): Int
}
