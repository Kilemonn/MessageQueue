package au.kilemon.messagequeue.message

import au.kilemon.messagequeue.queue.QueueTypeProvider
import lombok.EqualsAndHashCode
import java.io.Serializable

/**
 * A base [QueueMessage] object which will wrap any object that is placed into the [MultiQueue].
 * This object wraps a [Serializable] type `T` which is the payload to be stored in the queue.
 *
 * @author github.com/KyleGonzalez
 */
@EqualsAndHashCode
data class QueueMessage(val data: Serializable?, val type: QueueTypeProvider, @EqualsAndHashCode.Exclude var isConsumed: Boolean = false, @EqualsAndHashCode.Exclude var consumedBy: String? = null): Serializable
