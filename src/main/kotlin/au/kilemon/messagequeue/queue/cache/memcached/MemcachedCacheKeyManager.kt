package au.kilemon.messagequeue.queue.cache.memcached

import au.kilemon.messagequeue.queue.cache.CacheKeyManager
import net.rubyeye.xmemcached.MemcachedClient
import org.springframework.beans.factory.annotation.Autowired

/**
 * To optimise how we determine the key list/sub queue list when using a cache, this class is used to store and
 * manage the subqueue list for the [MemcachedMultiQueue].
 *
 * @author github.com/Kilemonn
 */
class MemcachedCacheKeyManager: CacheKeyManager()
{
    @Autowired
    private lateinit var client: MemcachedClient

    private fun persistUpdatedKeysSet(keys: HashSet<String>)
    {
        client.set(getReservedKey(), 0, keys)
    }

    override fun add(key: String)
    {
        val keys = getKeys()
        keys.add(key)
        persistUpdatedKeysSet(keys)
    }

    override fun remove(key: String)
    {
        val keys = getKeys()
        if (keys.remove(key))
        {
            persistUpdatedKeysSet(keys)
        }
    }

    override fun getKeys(): HashSet<String>
    {
        var keys: HashSet<String>? = client.get(getReservedKey())
        if (keys == null)
        {
            keys = HashSet()
        }
        return keys
    }

    override fun contains(key: String): Boolean
    {
        return getKeys().contains(key)
    }

    override fun clear()
    {
        persistUpdatedKeysSet(HashSet())
    }

}
