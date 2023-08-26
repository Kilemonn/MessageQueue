package au.kilemon.messagequeue.message

import java.io.Serializable

interface PayloadResolvableMessage<T>: Serializable
{
    fun resolvePayloadObject(): T

    fun removePayload(detailed: Boolean?): T
}
