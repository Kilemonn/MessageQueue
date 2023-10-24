package au.kilemon.messagequeue.authentication.authenticator.cache.redis

import au.kilemon.messagequeue.authentication.AuthenticationMatrix
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

/**
 *
 *
 * @author github.com/Kilemonn
 */
class RedisAuthenticator: MultiQueueAuthenticator()
{
    companion object
    {
        // TODO - Completely black list this key as its used for special purpose for redis
        const val RESTRICTED_KEY = AuthenticationMatrix.TABLE_NAME + "_restricted"
    }

    override val LOG: Logger = initialiseLogger()

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, AuthenticationMatrix>

    override fun isRestrictedInternal(subQueue: String): Boolean
    {
        return redisTemplate.opsForSet().members(RESTRICTED_KEY)?.contains(AuthenticationMatrix(subQueue)) ?: false
    }

    override fun addRestrictedEntryInternal(subQueue: String)
    {
        redisTemplate.opsForSet().add(RESTRICTED_KEY, AuthenticationMatrix(subQueue))
    }

    override fun removeRestrictionInternal(subQueue: String): Boolean
    {
        return redisTemplate.opsForSet().remove(RESTRICTED_KEY, subQueue) != null
    }
}
