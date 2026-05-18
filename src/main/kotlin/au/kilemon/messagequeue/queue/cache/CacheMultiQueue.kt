package au.kilemon.messagequeue.queue.cache

import java.util.stream.Collectors

/**
 * A marker interface for Cache backed [au.kilemon.messagequeue.queue.MultiQueue].
 *
 * @author github.com/Kilemonn
 */
interface CacheMultiQueue
{
    /**
     * Append the [getPrefix] to the provided [subQueue] [String].
     *
     * @param subQueue the [String] to add the prefix to
     * @return a [String] with the provided [subQueue] with the [getPrefix] appended to the beginning.
     */
    fun appendPrefix(subQueue: String): String
    {
        if (hasPrefix() && !subQueue.startsWith(getPrefix()))
        {
            return "${getPrefix()}$subQueue"
        }
        return subQueue
    }

    /**
     * @return whether the [getPrefix] is [String.isNotBlank]
     */
    fun hasPrefix(): Boolean
    {
        return getPrefix().isNotBlank()
    }

    /**
     * If [getPrefix] is set, removes this from all provided [keys].
     * If [getPrefix] is null or blank, then the provided [keys] [Set] is immediately returned.
     *
     * @param keys the [Set] of [String] to remove the [getPrefix] from
     * @return the updated [Set] of [String] with the [getPrefix] removed
     */
    fun removePrefix(keys: Set<String>): Set<String>
    {
        if (!hasPrefix())
        {
            return keys
        }

        val prefixLength = getPrefix().length
        return keys.stream().filter { key -> key.startsWith(getPrefix()) }
            .map { key -> key.substring(prefixLength) }
            .collect(Collectors.toSet())
    }

    fun getPrefix(): String
}
