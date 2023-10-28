package au.kilemon.messagequeue.authentication.authenticator.nosql.mongo

import au.kilemon.messagequeue.authentication.AuthenticationMatrixDocument
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired

/**
 * A [MultiQueueAuthenticator] implementation using MongoDB as the storage mechanism for the restricted sub-queue
 * identifiers.
 *
 * @author github.com/Kilemonn
 */
class MongoAuthenticator: MultiQueueAuthenticator()
{
    override val LOG: Logger = initialiseLogger()

    @Autowired
    lateinit var authenticationMatrixRepository: MongoAuthenticationMatrixRepository

    override fun isRestrictedInternal(subQueue: String): Boolean
    {
        val entries = authenticationMatrixRepository.findBySubQueue(subQueue)
        return entries.isNotEmpty()
    }

    /**
     * Since mongodb does not manage self generated IDs we need to generate the ID ourselves when creating a new entry.
     */
    private fun getNextQueueIndex(): Long
    {
        val largestIdMessage = authenticationMatrixRepository.findTopByOrderByIdDesc()
        return if (largestIdMessage.isPresent)
        {
            largestIdMessage.get().id?.plus(1) ?: 1
        }
        else
        {
            1
        }
    }

    override fun addRestrictedEntryInternal(subQueue: String)
    {
        val authMatrix = AuthenticationMatrixDocument(subQueue)
        authMatrix.id = getNextQueueIndex()
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
            .toList().toSet()
    }

    override fun clearRestrictedSubQueues(): Long
    {
        val existingEntriesCount = authenticationMatrixRepository.count()
        authenticationMatrixRepository.deleteAll()
        return existingEntriesCount
    }
}
