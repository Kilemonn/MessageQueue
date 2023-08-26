package au.kilemon.messagequeue.queue.sql.repository

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.repository.QueueMessageRepository
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
interface SQLQueueMessageRepository: JpaRepository<QueueMessage, Long>, QueueMessageRepository
{
    @Modifying
    @Transactional
    @Query("DELETE FROM QueueMessage WHERE type = ?1")
    override fun deleteByType(type: String): Int

    @Transactional
    @Query("SELECT DISTINCT type FROM QueueMessage")
    override fun findDistinctType(): List<String>

    @Transactional
    override fun findByTypeOrderByIdAsc(type: String): List<QueueMessage>

    @Transactional
    override fun findByTypeAndAssignedToIsNotNullOrderByIdAsc(type: String): List<QueueMessage>

    @Transactional
    override fun findByTypeAndAssignedToIsNullOrderByIdAsc(type: String): List<QueueMessage>

    @Transactional
    override fun findByTypeAndAssignedToOrderByIdAsc(type: String, assignedTo: String): List<QueueMessage>

    @Transactional
    override fun findByUuid(uuid: String): Optional<QueueMessage>

    @Modifying
    @Transactional
    override fun deleteByUuid(uuid: String): Int
}
