package au.kilemon.messagequeue.message

import lombok.EqualsAndHashCode
import java.io.Serializable

/**
 * A Message object which will wrap any object that is placed into the [MultiQueue].
 * This object wraps a [Serializable] type `T` which is the payload to be stored in the queue.
 *
 * @author github.com/KyleGonzalez
 */
@EqualsAndHashCode
data class Message<T: Serializable>(val data: T?, val type: MessageType, @EqualsAndHashCode.Exclude var isConsumed: Boolean = false, @EqualsAndHashCode.Exclude var consumedBy: String? = null): Serializable
