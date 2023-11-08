package au.kilemon.messagequeue.authentication.authenticator.inmemory

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.slf4j.Logger

/**
 * A [MultiQueueAuthenticator] implementation using an in-memory set as the storage mechanism for the restricted
 * sub-queue identifiers.
 *
 * @author github.com/Kilemonn
 */
class InMemoryAuthenticator: MultiQueueAuthenticator()
{
    override val LOG: Logger = this.initialiseLogger()

    private val restrictedSubQueues = HashSet<String>()

    override fun isRestrictedInternal(subQueue: String): Boolean
    {
        return restrictedSubQueues.contains(subQueue)
    }

    override fun addRestrictedEntryInternal(subQueue: String)
    {
        restrictedSubQueues.add(subQueue)
    }

    override fun removeRestrictionInternal(subQueue: String): Boolean
    {
        return restrictedSubQueues.remove(subQueue)
    }

    override fun getRestrictedSubQueueIdentifiers(): Set<String>
    {
        return restrictedSubQueues.toSet()
    }

    override fun clearRestrictedSubQueues(): Long
    {
        val existingEntriesSize = restrictedSubQueues.size.toLong()
        restrictedSubQueues.clear()
        return existingEntriesSize
    }
}
