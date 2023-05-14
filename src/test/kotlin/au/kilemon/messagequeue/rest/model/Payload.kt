package au.kilemon.messagequeue.rest.model

import java.io.Serializable

/**
 * A payload class to be used in testing.
 *
 * @author github.com/Kilemonn
 */
data class Payload(var data: String, var num: Int, var bool: Boolean, var enum: PayloadEnum): Serializable
