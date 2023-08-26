package au.kilemon.messagequeue.queue.nosql.mongo.repository

import au.kilemon.messagequeue.message.QueueMessageDocument
import au.kilemon.messagequeue.queue.repository.QueueMessageRepository
import org.springframework.data.mongodb.repository.MongoRepository


interface MongoQueueMessageRepository: MongoRepository<QueueMessageDocument, Long>, QueueMessageRepository
{

}
