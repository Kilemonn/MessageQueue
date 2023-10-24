package au.kilemon.messagequeue.authentication.authenticator.nosql.mongo

import au.kilemon.messagequeue.authentication.AuthenticationMatrix
import au.kilemon.messagequeue.authentication.AuthenticationMatrixDocument
import org.springframework.data.mongodb.repository.MongoRepository

/**
 *
 *
 * @author github.com/Kilemonn
 */
interface MongoAuthenticationMatrixRepository: MongoRepository<AuthenticationMatrixDocument, Long>
{
    fun findBySubQueue(subQueue: String): List<AuthenticationMatrixDocument>
}
