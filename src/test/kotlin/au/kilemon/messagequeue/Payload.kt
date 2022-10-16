package au.kilemon.messagequeue

import java.io.Serializable

/**
 * A payload class to be used in testing.
 *
 * @author github.com/KyleGonzalez
 */
data class Payload(var data: String, var num: Int, var bool: Boolean, var enum: PayloadEnum): Serializable
