package au.kilemon.messagequeue.authentication.authenticator.inmemory

import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.slf4j.Logger

/**
 *
 *
 * @author github.com/Kilemonn
 */
class InMemoryAuthenticator: MultiQueueAuthenticator()
{
    override val LOG: Logger = initialiseLogger()

    private val authMap = HashSet<String>()

    override fun isRestrictedInternal(subQueue: String): Boolean
    {
        return authMap.contains(subQueue)
    }

    override fun addRestrictedEntryInternal(subQueue: String)
    {
        authMap.add(subQueue)
    }

    override fun removeRestrictionInternal(subQueue: String): Boolean
    {
        return authMap.remove(subQueue)
    }
}
