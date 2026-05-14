package au.kilemon.messagequeue.configuration.cache.redis

/**
 * An enum class used to represent the different modes that `Redis` can be configured as and how we need
 * to connect to it.
 *
 * @author github.com/Kilemonn
 */
enum class RedisMode
{
    /**
     * Connecting to a standalone configured redis environment.
     */
    STANDALONE,

    /**
     * Connecting to a sentinel configured redis environment.
     */
    SENTINEL,

    /**
     * Connecting to a cluster configured redis environment.
     */
    CLUSTER;
}
