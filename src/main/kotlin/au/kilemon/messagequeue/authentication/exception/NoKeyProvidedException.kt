package au.kilemon.messagequeue.authentication.exception

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.settings.MessageQueueSettings

/**
 * An exception used when no [au.kilemon.messagequeue.authentication.token.JwtTokenProvider.tokenKey] is provided
 * and the queue is not in [au.kilemon.messagequeue.authentication.RestrictionMode.NONE] mode.
 *
 * @author github.com/Kilemonn
 */
class NoKeyProvidedException(restrictionMode: RestrictionMode) : Exception(String.format(MESSAGE_FORMAT, MessageQueueSettings.ACCESS_TOKEN_KEY, restrictionMode))
{
    companion object
    {
        const val MESSAGE_FORMAT = "Property [%s] is required when application is in [%s] mode."
    }
}
