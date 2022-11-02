package au.kilemon.messagequeue.queue.sql.repository

import au.kilemon.messagequeue.message.QueueMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QueueMessageRepository: JpaRepository<QueueMessage, Int>
