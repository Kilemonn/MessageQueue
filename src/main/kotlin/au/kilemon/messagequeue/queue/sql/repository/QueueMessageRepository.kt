package au.kilemon.messagequeue.queue.sql.repository

import au.kilemon.messagequeue.message.QueueMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface QueueMessageRepository: JpaRepository<QueueMessage, Int>
{
    @Modifying
    @Query("delete from ${QueueMessage.TABLE_NAME} queue where queue.type == ?1")
    fun deleteByType(type: String): Int

    fun findDistinctType(): List<String>
}
