package au.kilemon.messagequeue.authentication.authenticator.sql

import au.kilemon.messagequeue.authentication.AuthenticationMatrix
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired

/**
 *
 *
 * @author github.com/Kilemonn
 */
class SqlAuthenticator: MultiQueueAuthenticator()
{
    override val LOG: Logger = initialiseLogger()

    @Autowired
    lateinit var authenticationMatrixRepository: SqlAuthenticationMatrixRepository

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
        TODO("Not yet implemented")
    }

    override fun clearRestrictedSubQueues(): Long
    {
        val existingEntriesCount = authenticationMatrixRepository.count()
        authenticationMatrixRepository.deleteAll()
        return existingEntriesCount
    }
}
