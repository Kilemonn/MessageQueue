package au.kilemon.messagequeue.authentication.authenticator.cache.memcached

import au.kilemon.messagequeue.authentication.AuthenticationMatrix
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.cache.redis.RedisAuthenticator.Companion.RESTRICTED_KEY
import net.rubyeye.xmemcached.MemcachedClient
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Collectors

/**
 * A [MultiQueueAuthenticator] implementation using Memcached as the storage mechanism for the restricted
 * sub-queue identifiers.
 *
 * @author github.com/Kilemonn
 */
class MemcachedAuthenticator: MultiQueueAuthenticator()
{
    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    private lateinit var client: MemcachedClient

    override fun isRestrictedInternal(subQueue: String): Boolean
    {
        return getMembersSet().contains(AuthenticationMatrix(subQueue))
    }

    override fun addRestrictedEntryInternal(subQueue: String)
    {
        val restricted = getMembersSet()
        val added = restricted.add(AuthenticationMatrix(subQueue))
        if (added)
        {
            client.set(RESTRICTED_KEY, 0, restricted)
        }
    }

    override fun removeRestrictionInternal(subQueue: String): Boolean
    {
        val restricted = getMembersSet()
        val removed = restricted.remove(AuthenticationMatrix(subQueue))
        if (removed)
        {
            client.set(RESTRICTED_KEY, 0, restricted)
        }
        return removed
    }

    /**
     * Overriding to completely remove all access to the [RESTRICTED_KEY].
     * Only if [isInNoneMode] returns `false`.
     */
    override fun getReservedSubQueues(): Set<String>
    {
        if (!isInNoneMode())
        {
            return setOf(RESTRICTED_KEY)
        }
        return setOf()
    }

    override fun getRestrictedSubQueueIdentifiers(): Set<String>
    {
        return getMembersSet().stream().map { authMatrix -> authMatrix.subQueue }.collect(Collectors.toSet())
    }

    private fun getMembersSet(): HashSet<AuthenticationMatrix>
    {
        return client.get<HashSet<AuthenticationMatrix>>(RESTRICTED_KEY) ?: HashSet()
    }

    override fun clearRestrictedSubQueues(): Long
    {
        val members = getMembersSet()
        val existingMembersSize = members.size.toLong()
        client.delete(RESTRICTED_KEY)
        return existingMembersSize
    }
}
