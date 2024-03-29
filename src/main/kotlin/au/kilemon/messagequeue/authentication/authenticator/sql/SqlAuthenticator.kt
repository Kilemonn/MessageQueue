package au.kilemon.messagequeue.authentication.authenticator.sql

import au.kilemon.messagequeue.authentication.AuthenticationMatrix
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Collectors

/**
 * A [MultiQueueAuthenticator] implementation using SQL as the storage mechanism for the restricted sub-queue
 * identifiers.
 *
 * @author github.com/Kilemonn
 */
class SqlAuthenticator: MultiQueueAuthenticator()
{
    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    private lateinit var authenticationMatrixRepository: SqlAuthenticationMatrixRepository

    override fun isRestrictedInternal(subQueue: String): Boolean
    {
        val entries = authenticationMatrixRepository.findBySubQueue(subQueue)
        return entries.isNotEmpty()
    }

    override fun addRestrictedEntryInternal(subQueue: String)
    {
        val authMatrix = AuthenticationMatrix(subQueue)
        authenticationMatrixRepository.save(authMatrix)
    }

    override fun removeRestrictionInternal(subQueue: String): Boolean
    {
        val entries = authenticationMatrixRepository.findBySubQueue(subQueue)
        val entriesExist = entries.isNotEmpty()
        entries.forEach { entry -> authenticationMatrixRepository.delete(entry) }

        return entriesExist
    }

    override fun getRestrictedSubQueueIdentifiers(): Set<String>
    {
        return authenticationMatrixRepository.findAll().stream().map { authMatrix -> authMatrix.subQueue }
            .collect(Collectors.toSet())
    }

    override fun clearRestrictedSubQueues(): Long
    {
        val existingEntriesCount = authenticationMatrixRepository.count()
        authenticationMatrixRepository.deleteAll()
        return existingEntriesCount
    }
}
