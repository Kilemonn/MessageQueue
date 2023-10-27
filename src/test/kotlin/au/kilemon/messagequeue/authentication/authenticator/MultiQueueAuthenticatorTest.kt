package au.kilemon.messagequeue.authentication.authenticator

import au.kilemon.messagequeue.authentication.MultiQueueAuthenticationType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.SpyBean

/**
 * An abstract test class for the [MultiQueueAuthenticator] class.
 * This class can be extended, and the [MultiQueueAuthenticator] member overridden to easily ensure that the different
 * [MultiQueueAuthenticator] implementations all operate as expected in the same test cases.
 *
 * @author github.com/Kilemonn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class MultiQueueAuthenticatorTest
{
    @SpyBean
    protected lateinit var multiQueueAuthenticator: MultiQueueAuthenticator

    @BeforeEach
    fun setUp()
    {
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }

    /**
     * Ensure [MultiQueueAuthenticator.addRestrictedEntry] always returns and does not add an entry if the
     * [MultiQueueAuthenticator.getAuthenticationType] is [MultiQueueAuthenticationType.NONE].
     */
    @Test
    fun testAddRestrictedEntry_WithNoneMode()
    {
        Assertions.assertEquals(MultiQueueAuthenticationType.NONE, multiQueueAuthenticator.getAuthenticationType())
        val subQueue = "testAddRestrictedEntry_WithNoneMode"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure [MultiQueueAuthenticator.addRestrictedEntry] will add the request sub queue identifier when the
     * [MultiQueueAuthenticator.getAuthenticationType] is NOT [MultiQueueAuthenticationType.NONE].
     * Also tests [MultiQueueAuthenticator.isRestricted] can determine the sub queue is restricted after its been added.
     */
    @Test
    fun testAddRestrictedEntry_WithARestrictedNoneMode()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.RESTRICTED).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.RESTRICTED, multiQueueAuthenticator.getAuthenticationType())
        val subQueue = "testAddRestrictedEntry_WithARestrictedNoneMode"

        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.removeRestriction] will not be able to remove a restriction if the
     * [MultiQueueAuthenticator.getAuthenticationType] is set to [MultiQueueAuthenticationType.NONE].
     */
    @Test
    fun testRemoveRestriction_WithNoneMode()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.RESTRICTED).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.RESTRICTED, multiQueueAuthenticator.getAuthenticationType())
        val subQueue = "testRemoveRestriction_WithNoneMode"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        Mockito.doReturn(MultiQueueAuthenticationType.NONE).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.NONE, multiQueueAuthenticator.getAuthenticationType())
        Assertions.assertFalse(multiQueueAuthenticator.removeRestriction(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.addRestrictedEntry] returns `false` when an existing sub queue identifier
     * is attempting to be added.
     */
    @Test
    fun testRemoveRestriction_AddExistingSubQueueIdentifier()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.RESTRICTED).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.RESTRICTED, multiQueueAuthenticator.getAuthenticationType())
        val subQueue = "testRemoveRestriction_AddExistingSubQueueIdentifier"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertFalse(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.removeRestriction] will not be able to remove a restriction if the
     * [MultiQueueAuthenticator.getAuthenticationType] is set to [MultiQueueAuthenticationType.NONE].
     */
    @Test
    fun testRemoveRestriction_WithARestrictedNoneMode()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.RESTRICTED).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.RESTRICTED, multiQueueAuthenticator.getAuthenticationType())
        val subQueue = "testRemoveRestriction_WithARestrictedNoneMode"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        multiQueueAuthenticator.addRestrictedEntry(subQueue)
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertTrue(multiQueueAuthenticator.removeRestriction(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.removeRestriction] returns `false` when you attempt to remove a sub queue
     * identifier that does not exist.
     */
    @Test
    fun testRemoveRestriction_DoesNotExist()
    {
        Mockito.doReturn(MultiQueueAuthenticationType.RESTRICTED).`when`(multiQueueAuthenticator).getAuthenticationType()
        Assertions.assertEquals(MultiQueueAuthenticationType.RESTRICTED, multiQueueAuthenticator.getAuthenticationType())
        val subQueue = "testRemoveRestriction_DoesNotExist"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.removeRestriction(subQueue))
    }
}
