package au.kilemon.messagequeue.queue.cache

/**
 * To optimise how we determine the key list/sub queue list when using a cache, this class is used to store and
 * manage the subqueue list for the cache backed [au.kilemon.messagequeue.queue.MultiQueue].
 *
 * @author github.com/Kilemonn
 */
abstract class CacheKeyManager(protected val prefix: String = "")
{
    companion object
    {
        const val CACHE_KEYS_KEY: String = "messagequeue-cache-keys"
    }

    fun getReservedKeys(): Set<String>
    {
        return setOf("$prefix$CACHE_KEYS_KEY")
    }

    abstract fun add(key: String)

    abstract fun remove(key: String)

    abstract fun getKeys(): HashSet<String>

    /**
     * Used for tests.
     */
    abstract fun contains(key: String): Boolean

    /**
     * Used for tests.
     */
    abstract fun clear()
}