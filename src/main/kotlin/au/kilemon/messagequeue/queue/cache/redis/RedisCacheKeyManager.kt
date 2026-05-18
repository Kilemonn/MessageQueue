package au.kilemon.messagequeue.queue.cache.redis

import au.kilemon.messagequeue.queue.cache.CacheKeyManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

/**
 * To optimise how we determine the key list/sub queue list when using a cache, this class is used to store and
 * manage the subqueue list for the [RedisMultiQueue].
 *
 * @author github.com/Kilemonn
 */
class RedisCacheKeyManager: CacheKeyManager()
{
    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    override fun add(key: String)
    {
        redisTemplate.opsForSet().add(CACHE_KEYS_KEY, key)
    }

    override fun remove(key: String)
    {
        redisTemplate.opsForSet().remove(CACHE_KEYS_KEY, key)
    }

    override fun contains(key: String): Boolean
    {
        return redisTemplate.opsForSet().isMember(CACHE_KEYS_KEY, key)
    }

    override fun getKeys(): HashSet<String>
    {
        return HashSet(redisTemplate.opsForSet().members(CACHE_KEYS_KEY))
    }

    override fun clear()
    {
        redisTemplate.delete(CACHE_KEYS_KEY)
    }
}
