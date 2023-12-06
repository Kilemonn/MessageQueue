package au.kilemon.messagequeue.authentication.authenticator.nosql.mongo

import au.kilemon.messagequeue.authentication.AuthenticationMatrixDocument
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

/**
 * A [MongoRepository] for [AuthenticationMatrixDocument] which stores which sub-queues are under restricted access.
 *
 * @author github.com/Kilemonn
 */
interface MongoAuthenticationMatrixRepository: MongoRepository<AuthenticationMatrixDocument, Long>
{
    fun findBySubQueue(subQueue: String): List<AuthenticationMatrixDocument>

    /**
     * Get the entry with the largest ID.
     *
     * @return the [AuthenticationMatrixDocument] with the largest ID, otherwise [Optional.empty]
     */
    fun findTopByOrderByIdDesc(): Optional<AuthenticationMatrixDocument>
}
