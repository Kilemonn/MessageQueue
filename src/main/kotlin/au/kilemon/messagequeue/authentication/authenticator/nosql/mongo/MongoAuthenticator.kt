package au.kilemon.messagequeue.authentication.authenticator.nosql.mongo

import au.kilemon.messagequeue.authentication.AuthenticationMatrixDocument
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Collectors

/**
 * A [MultiQueueAuthenticator] implementation using MongoDB as the storage mechanism for the restricted sub-queue
 * identifiers.
 *
 * @author github.com/Kilemonn
 */
class MongoAuthenticator: MultiQueueAuthenticator()
{
    override val LOG: Logger = this.initialiseLogger()

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
            var lastIndex = largestIdMessage.get().id
            if (lastIndex == null)
            {
                val index = 1L
                LOG.warn("Returning [{}] as next index, an auth matrix entry was found but its ID was null.", index)
                return index
            }
            else
            {
                lastIndex++
                LOG.trace("Returning [{}] as next index.", lastIndex)
                return lastIndex
            }
        }
        else
        {
            val index = 1L
            LOG.trace("Returning [{}] as next index since there are no existing auth matrix entries.", index)
            index
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
            .collect(Collectors.toSet())
    }

    override fun clearRestrictedSubQueues(): Long
    {
        val existingEntriesCount = authenticationMatrixRepository.count()
        authenticationMatrixRepository.deleteAll()
        return existingEntriesCount
    }
}
