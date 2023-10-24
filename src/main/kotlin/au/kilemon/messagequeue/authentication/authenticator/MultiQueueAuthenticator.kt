package au.kilemon.messagequeue.authentication.authenticator

import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import au.kilemon.messagequeue.logging.HasLogger
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired

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
     *
     */
    fun isRestricted(subQueue: String): Boolean
    {
        return if (multiQueueAuthenticationType == MultiQueueAuthenticationType.NONE)
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
        if (multiQueueAuthenticationType != MultiQueueAuthenticationType.NONE)
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
        if (multiQueueAuthenticationType != MultiQueueAuthenticationType.NONE)
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
