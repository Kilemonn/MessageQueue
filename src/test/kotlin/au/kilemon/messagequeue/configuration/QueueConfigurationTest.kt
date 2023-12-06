package au.kilemon.messagequeue.configuration

import au.kilemon.messagequeue.authentication.RestrictionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for [QueueConfiguration].
 *
 * @author github.com/Kilemonn
 */
class QueueConfigurationTest
{
    private val queueConfiguration: QueueConfiguration = QueueConfiguration()

    /**
     * Test that [QueueConfiguration.parseRestrictionMode] parses to the correct [RestrictionMode].
     */
    @Test
    fun testGetRestrictionMode()
    {
        val mode = RestrictionMode.RESTRICTED
        val restrictionMode = queueConfiguration.parseRestrictionMode(mode.toString())
        Assertions.assertEquals(mode, restrictionMode)
    }

    /**
     * Test that [QueueConfiguration.parseRestrictionMode] returns [RestrictionMode.NONE] when the provided value
     * is not a [RestrictionMode].
     */
    @Test
    fun testGetRestrictionMode_invalidMode()
    {
        val restrictionMode = queueConfiguration.parseRestrictionMode("My mode")
        Assertions.assertEquals(RestrictionMode.NONE, restrictionMode)
    }

    /**
     * Test that [QueueConfiguration.parseRestrictionMode] returns [RestrictionMode.NONE] when the provided value
     * is an empty string.
     */
    @Test
    fun testGetRestrictionMode_modeIsBlank()
    {
        val restrictionMode = queueConfiguration.parseRestrictionMode("")
        Assertions.assertEquals(RestrictionMode.NONE, restrictionMode)
    }
}
