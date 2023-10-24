package au.kilemon.messagequeue.authentication

/**
 * An enum class used to represent the different types of `MultiQueueAuthentication`.
 * This will drive whether authentication is available and for which sub queues.
 *
 * @author github.com/Kilemonn
 */
enum class MultiQueueAuthenticationType
{
    /**
     * This is the default, which enforces no authentication on any sub queue, messages can be enqueued and dequeued
     * as required by any called without any form of authentication.
     */
    NONE,

    /**
     * This is a hybrid mode where sub queues can be created without authentication, but other sub queues can
     * be created with it (if they do not already exist).
     * Any sub queue created with authentication will not be accessible without a token, sub queues created without a
     * token will continue to be accessible without one.
     */
    HYBRID,

    /**
     * This is a restricted mode that forces any sub queue to be pre-created and a token will be given before messages
     * can be stored or accessed in any sub queue.
     */
    RESTRICTED;
}
