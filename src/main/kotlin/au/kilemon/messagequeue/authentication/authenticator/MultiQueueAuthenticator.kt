package au.kilemon.messagequeue.authentication.authenticator

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthorisationException
import au.kilemon.messagequeue.filter.JwtAuthenticationFilter
import au.kilemon.messagequeue.logging.HasLogger
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired

/**
 * The base Authenticator class. This is responsible to tracking which sub-queues are marked as restricted and
 * maintaining this underlying collection within the specified storage medium.
 *
 * @author github.com/Kilemonn
 */
abstract class MultiQueueAuthenticator: HasLogger
{
    abstract override val LOG: Logger

    @Autowired
    private lateinit var restrictionMode: RestrictionMode

    /**
     * @return [restrictionMode]
     */
    fun getRestrictionMode(): RestrictionMode
    {
        return restrictionMode
    }

    /**
     * Used only for tests to update the set [RestrictionMode].
     *
     * @param restrictionMode the new [RestrictionMode] to set
     */
    fun setRestrictionMode(restrictionMode: RestrictionMode)
    {
        this.restrictionMode = restrictionMode
    }

    /**
     * Used to return a list of completed reserved sub-queue identifiers that can never be used. Even when
     * [RestrictionMode.NONE] is being used.
     *
     * @return list of sub-queue identifiers that cannot be used
     */
    open fun getReservedSubQueues(): Set<String>
    {
        return setOf()
    }

    /**
     * Determines whether based on the currently set [getRestrictionMode] and the provided [subQueue] and
     * [JwtAuthenticationFilter.getSubQueue] to determine if the user is able to interact with the requested sub-queue.
     *
     * @param subQueue the sub-queue identifier that is being requested access to
     * @param throwException `true` to throw an exception if the [subQueue] cannot be accessed, otherwise the return
     * value can be used
     * @return returns `true` if the [subQueue] can be accessed, otherwise `false`
     * @throws MultiQueueAuthorisationException if there is a mis-matching token OR no token provided. Or the sub-queue
     * is not in restricted mode when it should be
     */
    @Throws(MultiQueueAuthorisationException::class)
    fun canAccessSubQueue(subQueue: String, throwException: Boolean = true): Boolean
    {
        if (getReservedSubQueues().contains(subQueue))
        {
            if (throwException)
            {
                throw MultiQueueAuthorisationException(subQueue, getRestrictionMode())
            }
            return false
        }

        if (isInNoneMode())
        {
            return true
        }
        else if (isInHybridMode())
        {
            if (isRestricted(subQueue))
            {
                if (JwtAuthenticationFilter.getSubQueue() == subQueue)
                {
                    return true
                }
            }
            else
            {
                // If we are in hybrid mode and the sub-queue is not restricted we should let it pass
                return true
            }
        }
        else if (isInRestrictedMode())
        {
            if (isRestricted(subQueue) && JwtAuthenticationFilter.getSubQueue() == subQueue)
            {
                return true
            }
        }

        if (throwException)
        {
            throw MultiQueueAuthorisationException(subQueue, getRestrictionMode())
        }
        return false
    }

    /**
     * Indicates whether [restrictionMode] is set to [RestrictionMode.NONE].
     */
    fun isInNoneMode(): Boolean
    {
        return getRestrictionMode() == RestrictionMode.NONE
    }

    /**
     * Indicates whether [restrictionMode] is set to [RestrictionMode.HYBRID].
     */
    fun isInHybridMode(): Boolean
    {
        return getRestrictionMode() == RestrictionMode.HYBRID
    }

    /**
     * Indicates whether [restrictionMode] is set to [RestrictionMode.RESTRICTED].
     */
    fun isInRestrictedMode(): Boolean
    {
        return getRestrictionMode() == RestrictionMode.RESTRICTED
    }

    /**
     * Will determine if the requested [subQueue] identifier is being treated as a restricted queue or not.
     *
     * @param subQueue the sub-queue to check whether it is restricted or not
     * @return if [isInNoneMode] will always return `false`, otherwise delegates to [isRestrictedInternal]
     */
    fun isRestricted(subQueue: String): Boolean
    {
        return if (isInNoneMode())
        {
            false
        }
        else
        {
            isRestrictedInternal(subQueue)
        }
    }

    /**
     * Defers to the child class to implement this. It should look up in the appropriate storage medium to determine
     * whether the provided [subQueue] is restricted or not.
     *
     * @param subQueue the sub-queue to check whether it is restricted or not
     * @return `true` if this sub-queue is in restricted mode, otherwise `false`
     */
    abstract fun isRestrictedInternal(subQueue: String): Boolean


    /**
     * Add the provided [subQueue] identifier as a restricted sub-queue.
     * This will delegate to [addRestrictedEntryInternal].
     *
     * @param subQueue the sub-queue identifier to make restricted
     * @return `true` if the sub-queue identifier was added to the restriction set, otherwise `false` if there was
     * no underlying change made. If [isInNoneMode] is set this will always return `false`.
     */
    fun addRestrictedEntry(subQueue: String): Boolean
    {
        if (isInNoneMode())
        {
            LOG.trace("Skipping adding restricted entry for [{}] since the restriction mode is set to [{}].", subQueue, getRestrictionMode())
            return false
        }
        else
        {
            return if (isRestricted(subQueue))
            {
                LOG.trace("Restriction for sub-queue [{}] was not increased as it is already restricted.", subQueue)
                false
            }
            else
            {
                LOG.info("Adding restriction to sub-queue [{}].", subQueue)
                addRestrictedEntryInternal(subQueue)
                true
            }
        }
    }

    /**
     * Add the provided [subQueue] identifier as a restricted sub-queue.
     *
     * @param subQueue the sub-queue identifier to make restricted
     */
    abstract fun addRestrictedEntryInternal(subQueue: String)

    /**
     * Remove the provided [subQueue] from being a restricted sub-queue.
     * This will delegate to [removeRestrictionInternal].
     *
     * @param subQueue the sub-queue identifier that will no longer be treated as restricted
     * @return `true` if there was a restriction that was removed because of this call, otherwise `false`. If
     * [isInNoneMode] this will always return `false`
     */
    fun removeRestriction(subQueue: String): Boolean
    {
        return if (isInNoneMode())
        {
            LOG.trace("Skipping removing restricted entry for [{}] since the restriction mode is set to [{}].", subQueue, getRestrictionMode())
            false
        }
        else
        {
            return if (isRestricted(subQueue))
            {
                LOG.info("Removing restriction to sub-queue [{}].", subQueue)
                removeRestrictionInternal(subQueue)
            }
            else
            {
                LOG.trace("Restriction for sub-queue [{}] was not removed as it is currently unrestricted.", subQueue)
                false
            }

        }
    }

    /**
     * Remove the provided [subQueue] from being a restricted sub-queue.
     *
     * @param subQueue the sub-queue identifier that will no longer be treated as restricted
     * @return `true` if the identifier is no longer marked as restricted, otherwise `false`
     */
    abstract fun removeRestrictionInternal(subQueue: String): Boolean

    /**
     * @return the underlying [Set] of sub-queue identifiers that are marked as restricted
     */
    abstract fun getRestrictedSubQueueIdentifiers(): Set<String>

    /**
     * Clear the underlying restriction storage entries. (This is mainly used for testing).
     *
     * @return the amount of sub-queue restrictions that were cleared
     */
    abstract fun clearRestrictedSubQueues(): Long
}
