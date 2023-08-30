package au.kilemon.messagequeue.queue.nosql.mongo.repository

import au.kilemon.messagequeue.message.QueueMessageDocument
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*


interface MongoQueueMessageRepository: MongoRepository<QueueMessageDocument, Long>
{
    /**
     *
     */
    @Aggregation(pipeline = [ "{ '\$group': { '_id' : '\$type' } }" ])
    fun getDistinctTypes(): List<String>

    /**
     *
     */
    fun findByTypeOrderByIdAsc(type: String): List<QueueMessageDocument>

    /**
     *
     */
    fun findByUuid(uuid: String): Optional<QueueMessageDocument>

    /**
     *
     */
    @Modifying
    fun deleteByType(type:String): Int

    /**
     *
     */
    @Modifying
    fun deleteByUuid(uuid: String): Int
}
