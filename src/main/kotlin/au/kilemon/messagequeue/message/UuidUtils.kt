package au.kilemon.messagequeue.message

import java.util.UUID

/**
 * UUID Utility functions.
 *
 * @author github.com/Kilemonn
 */
object UuidUtils
{
    /**
     * From the provided uuid v7 string this method parses the first 48 bits (the unix time portion)
     * and return that as a long. This is used for redis to score the queue message object based on the time it's
     * uuid was created.
     */
    fun getUuidEpochTimestamp(uuidString: String): Long
    {
        try
        {
            val uuid: UUID = UUID.fromString(uuidString)

            // In Kotlin `ushr` is an infix function that performs an unsigned right shift.
            // It shifts the bit pattern of a value to the right by a specified number of bits,
            // filling the leftmost bits with zeros, regardless of whether the original number
            // was positive or negative.
            return ((uuid.mostSignificantBits ushr 16) and 0xFFFFFFFFFFFFL)
        }
        catch (_: IllegalArgumentException)
        {

        }

        return -1
    }
}
