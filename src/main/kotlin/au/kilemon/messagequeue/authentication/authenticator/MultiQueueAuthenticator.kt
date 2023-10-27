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
    protected lateinit var multiQueueAuthenticationType: MultiQueueAuthenticationType

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

        throw MultiQueueAuthorisationException(subQueue, multiQueueAuthenticationType)
    }

    /**
     * Indicates whether [multiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.NONE].
     */
    fun isInNoneMode(): Boolean
    {
        return multiQueueAuthenticationType == MultiQueueAuthenticationType.NONE
    }

    /**
     * Indicates whether [multiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.HYBRID].
     */
    fun isInHybridMode(): Boolean
    {
        return multiQueueAuthenticationType == MultiQueueAuthenticationType.HYBRID
    }

    /**
     * Indicates whether [multiQueueAuthenticationType] is set to [MultiQueueAuthenticationType.RESTRICTED].
     */
    fun isInRestrictedMode(): Boolean
    {
        return multiQueueAuthenticationType == MultiQueueAuthenticationType.RESTRICTED
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
     *
     */
    fun addRestrictedEntry(subQueue: String)
    {
        if (isInNoneMode())
        {
            LOG.debug("Adding restriction level [{}] to sub queue [{}].", multiQueueAuthenticationType, subQueue)
            addRestrictedEntryInternal(subQueue)
        }
        else
        {
            LOG.trace("Bypassing adding restricted entry for [{}] since the authentication type is set to [{}].", subQueue, multiQueueAuthenticationType)
        }
    }

    /**
     *
     */
    abstract fun addRestrictedEntryInternal(subQueue: String)

    /**
     *
     */
    fun removeRestriction(subQueue: String): Boolean
    {
        if (isInNoneMode())
        {
            return removeRestrictionInternal(subQueue)
        }
        return false
    }

    /**
     *
     */
    abstract fun removeRestrictionInternal(subQueue: String): Boolean
}
