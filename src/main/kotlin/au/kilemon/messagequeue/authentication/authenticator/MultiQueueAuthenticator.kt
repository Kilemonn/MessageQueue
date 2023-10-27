package au.kilemon.messagequeue.authentication.authenticator

import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthorisationException
import au.kilemon.messagequeue.filter.JwtAuthenticationFilter
import au.kilemon.messagequeue.logging.HasLogger
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import kotlin.jvm.Throws

/**
 *
 *
 * @author github.com/Kilemonn
 */
abstract class MultiQueueAuthenticator: HasLogger
{
    abstract override val LOG: Logger

    @Autowired
    private lateinit var multiQueueAuthenticationType: MultiQueueAuthenticationType

    /**
     * @return [multiQueueAuthenticationType]
     */
    fun getAuthenticationType(): MultiQueueAuthenticationType
    {
        return multiQueueAuthenticationType
    }

    /**
     *
     */
    @Throws(MultiQueueAuthorisationException::class)
    fun canAccessSubQueue(subQueue: String)
    {
        if (isInNoneMode())
        {
            return
        }
        else if (isInHybridMode())
        {
            if (isRestricted(subQueue))
            {
                if (JwtAuthenticationFilter.getSubQueue() == subQueue)
                {
                    return
                }
            }
            else
            {
                // If we are in hybrid mode and the sub queue is not restricted we should let it pass
                return
            }
        }
        else if (isInRestrictedMode())
        {
            if (isRestricted(subQueue) && JwtAuthenticationFilter.getSubQueue() == subQueue)
            {
                return
            }
        }

        throw MultiQueueAuthorisationException(subQueue, getAuthenticationType())
    }

    /**
     * Indicates whether [multiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.NONE].
     */
    fun isInNoneMode(): Boolean
    {
        return getAuthenticationType() == MultiQueueAuthenticationType.NONE
    }

    /**
     * Indicates whether [multiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.HYBRID].
     */
    fun isInHybridMode(): Boolean
    {
        return getAuthenticationType() == MultiQueueAuthenticationType.HYBRID
    }

    /**
     * Indicates whether [multiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.RESTRICTED].
     */
    fun isInRestrictedMode(): Boolean
    {
        return getAuthenticationType() == MultiQueueAuthenticationType.RESTRICTED
    }

    /**
     *
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
     *
     */
    abstract fun isRestrictedInternal(subQueue: String): Boolean


    /**
     * @return `true` if the sub queue identifier was added to the restriction set, otherwise `false` if there was
     * no underlying change made
     */
    fun addRestrictedEntry(subQueue: String): Boolean
    {
        if (isInNoneMode())
        {
            LOG.trace("Skipping adding restricted entry for [{}] since the authentication type is set to [{}].", subQueue, getAuthenticationType())
            return false
        }
        else
        {
            return if (isRestricted(subQueue))
            {
                LOG.trace("Restriction for sub queue [{}] was not increased as it is already restricted.", subQueue)
                false
            }
            else
            {
                LOG.info("Adding restriction to sub queue [{}].", subQueue)
                addRestrictedEntryInternal(subQueue)
                true
            }
        }
    }

    /**
     *
     */
    abstract fun addRestrictedEntryInternal(subQueue: String)

    /**
     *  @return `true` if there was a restriction that was removed because of this call, otherwise `false`
     */
    fun removeRestriction(subQueue: String): Boolean
    {
        return if (isInNoneMode())
        {
            LOG.trace("Skipping removing restricted entry for [{}] since the authentication type is set to [{}].", subQueue, getAuthenticationType())
            false
        }
        else
        {
            return if (isRestricted(subQueue))
            {
                LOG.info("Removing restriction to sub queue [{}].", subQueue)
                removeRestrictionInternal(subQueue)
            }
            else
            {
                LOG.trace("Restriction for sub queue [{}] was not removed as it is currently unrestricted.", subQueue)
                false
            }

        }
    }

    /**
     *
     */
    abstract fun removeRestrictionInternal(subQueue: String): Boolean

    abstract fun getRestrictedSubQueueIdentifiers(): Set<String>

    /**
     * Clear the underlying restriction storage entries. (This is mainly used for testing).
     *
     * @return the amount of sub queue restrictions that were cleared
     */
    abstract fun clearRestrictedSubQueues(): Long
}
