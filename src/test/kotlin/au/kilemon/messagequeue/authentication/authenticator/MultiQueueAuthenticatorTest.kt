package au.kilemon.messagequeue.authentication.authenticator

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthorisationException
import au.kilemon.messagequeue.filter.JwtAuthenticationFilter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.slf4j.MDC
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

    /**
     * Ensure [MultiQueueAuthenticator.addRestrictedEntry] always returns and does not add an entry if the
     * [MultiQueueAuthenticator.getRestrictionMode] is [RestrictionMode.NONE].
     */
    @Test
    fun testAddRestrictedEntry_WithNoneMode()
    {
        Assertions.assertEquals(RestrictionMode.NONE, multiQueueAuthenticator.getRestrictionMode())
        val subQueue = "testAddRestrictedEntry_WithNoneMode"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure [MultiQueueAuthenticator.addRestrictedEntry] will add the request sub-queue identifier when the
     * [MultiQueueAuthenticator.getRestrictionMode] is NOT [RestrictionMode.NONE].
     * Also tests [MultiQueueAuthenticator.isRestricted] can determine the sub-queue is restricted after its been added.
     */
    @Test
    fun testAddRestrictedEntry_WithARestrictedNoneMode()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())
        val subQueue = "testAddRestrictedEntry_WithARestrictedNoneMode"

        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.removeRestriction] will not be able to remove a restriction if the
     * [MultiQueueAuthenticator.getRestrictionMode] is set to [RestrictionMode.NONE].
     */
    @Test
    fun testRemoveRestriction_WithNoneMode()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())
        val subQueue = "testRemoveRestriction_WithNoneMode"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        Mockito.doReturn(RestrictionMode.NONE).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.NONE, multiQueueAuthenticator.getRestrictionMode())
        Assertions.assertFalse(multiQueueAuthenticator.removeRestriction(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.addRestrictedEntry] returns `false` when an existing sub-queue identifier
     * is attempting to be added.
     */
    @Test
    fun testRemoveRestriction_AddExistingSubQueueIdentifier()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())
        val subQueue = "testRemoveRestriction_AddExistingSubQueueIdentifier"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertFalse(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.removeRestriction] will not be able to remove a restriction if the
     * [MultiQueueAuthenticator.getRestrictionMode] is set to [RestrictionMode.NONE].
     */
    @Test
    fun testRemoveRestriction_WithARestrictedNoneMode()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())
        val subQueue = "testRemoveRestriction_WithARestrictedNoneMode"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        multiQueueAuthenticator.addRestrictedEntry(subQueue)
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertTrue(multiQueueAuthenticator.removeRestriction(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.removeRestriction] returns `false` when you attempt to remove a sub-queue
     * identifier that does not exist.
     */
    @Test
    fun testRemoveRestriction_DoesNotExist()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())
        val subQueue = "testRemoveRestriction_DoesNotExist"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        Assertions.assertFalse(multiQueueAuthenticator.removeRestriction(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.canAccessSubQueue] never throws when its in
     * [MultiQueueAuthenticator.isInNoneMode].
     */
    @Test
    fun testCanAccessSubQueue_WithNoneMode()
    {
        Assertions.assertEquals(RestrictionMode.NONE, multiQueueAuthenticator.getRestrictionMode())
        val subQueue = "testCanAccessSubQueue_WithNoneMode"
        Assertions.assertTrue(multiQueueAuthenticator.canAccessSubQueue(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.canAccessSubQueue] never throws when its in
     * [MultiQueueAuthenticator.isInHybridMode] and the sub-queue identifier is not marked as restricted.
     */
    @Test
    fun testCanAccessSubQueue_WithHybridMode_isNotRestricted()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testCanAccessSubQueue_WithHybridMode_isNotRestricted"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))

        Assertions.assertTrue(multiQueueAuthenticator.canAccessSubQueue(subQueue))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.canAccessSubQueue] never throws when its in
     * [MultiQueueAuthenticator.isInHybridMode] and the sub-queue identifier is marked as restricted AND matches
     * the stored sub-queue identifier from the auth token.
     */
    @Test
    fun testCanAccessSubQueue_WithHybridMode_isRestricted_matchesStoredSubQueue()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testCanAccessSubQueue_WithHybridMode_isRestricted_matchesStoredSubQueue"
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        try
        {
            MDC.put(JwtAuthenticationFilter.SUB_QUEUE, subQueue)
            Assertions.assertTrue(multiQueueAuthenticator.canAccessSubQueue(subQueue))
        }
        finally
        {
            MDC.clear()
        }
    }

    /**
     * Ensure that [MultiQueueAuthenticator.canAccessSubQueue] DOES throw a [MultiQueueAuthorisationException] when its
     * in [MultiQueueAuthenticator.isInHybridMode] and the sub-queue identifier is marked as restricted AND does NOT
     * match the stored sub-queue identifier from the auth token.
     */
    @Test
    fun testCanAccessSubQueue_WithHybridMode_isRestricted_doesNotMatchStoredSubQueue()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testCanAccessSubQueue_WithHybridMode_isRestricted_doesNotMatchStoredSubQueue"
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        try
        {
            MDC.put(JwtAuthenticationFilter.SUB_QUEUE, "does not match sub-queue")
            Assertions.assertThrows(MultiQueueAuthorisationException::class.java) {
                multiQueueAuthenticator.canAccessSubQueue(subQueue)
            }
            Assertions.assertFalse(multiQueueAuthenticator.canAccessSubQueue(subQueue, false))
        }
        finally
        {
            MDC.clear()
        }
    }

    /**
     * Ensure that [MultiQueueAuthenticator.canAccessSubQueue] DOES throw a [MultiQueueAuthorisationException] when its
     * in [MultiQueueAuthenticator.isInRestrictedMode] and the sub-queue identifier is NOT marked as restricted.
     */
    @Test
    fun testCanAccessSubQueue_WithRestrictedMode_isNotRestricted()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testCanAccessSubQueue_WithRestrictedMode_isNotRestricted"
        Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue))
        Assertions.assertThrows(MultiQueueAuthorisationException::class.java) {
            multiQueueAuthenticator.canAccessSubQueue(subQueue)
        }
        Assertions.assertFalse(multiQueueAuthenticator.canAccessSubQueue(subQueue, false))
    }

    /**
     * Ensure that [MultiQueueAuthenticator.canAccessSubQueue] does NOT throw when its
     * in [MultiQueueAuthenticator.isInRestrictedMode] and the sub-queue identifier is marked as restricted AND the
     * stored sub-queue identifier from the token matches the requested token.
     */
    @Test
    fun testCanAccessSubQueue_WithRestrictedMode_isRestricted_matchesStoredSubQueue()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testCanAccessSubQueue_WithRestrictedMode_isRestricted_matchesStoredSubQueue"
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        try
        {
            MDC.put(JwtAuthenticationFilter.SUB_QUEUE, subQueue)
            multiQueueAuthenticator.canAccessSubQueue(subQueue)
            Assertions.assertTrue(multiQueueAuthenticator.canAccessSubQueue(subQueue))
        }
        finally
        {
            MDC.clear()
        }
    }

    /**
     * Ensure that [MultiQueueAuthenticator.canAccessSubQueue] DOES throw a [MultiQueueAuthorisationException] when its
     * in [MultiQueueAuthenticator.isInRestrictedMode] and the sub-queue identifier is marked as restricted and the
     * provided sub-queue identifier does NOT match the identifier provided in the auth token.
     */
    @Test
    fun testCanAccessSubQueue_WithRestrictedMode_isRestricted_doesNotMatchStoredSubQueue()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        val subQueue = "testCanAccessSubQueue_WithRestrictedMode_isRestricted_doesNotMatchStoredSubQueue"
        Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue))

        try
        {
            MDC.put(JwtAuthenticationFilter.SUB_QUEUE, "does not match sub-queue")
            Assertions.assertThrows(MultiQueueAuthorisationException::class.java) {
                multiQueueAuthenticator.canAccessSubQueue(subQueue)
            }
            Assertions.assertFalse(multiQueueAuthenticator.canAccessSubQueue(subQueue, false))
        }
        finally
        {
            MDC.clear()
        }
    }

    /**
     * Ensure that [MultiQueueAuthenticator.clearRestrictedSubQueues] will clear all sub-queue restrictions.
     */
    @Test
    fun testClearRestrictedSubQueues()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        val subQueues = listOf("testClearRestrictedSubQueues1", "testClearRestrictedSubQueues2", "testClearRestrictedSubQueues3",
            "testClearRestrictedSubQueues4", "testClearRestrictedSubQueues5", "testClearRestrictedSubQueues6")

        subQueues.forEach { subQueue -> Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue)) }
        subQueues.forEach { subQueue -> Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue)) }
        subQueues.forEach { subQueue -> Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue)) }
        multiQueueAuthenticator.clearRestrictedSubQueues()
        subQueues.forEach { subQueue -> Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue)) }
        Assertions.assertTrue(multiQueueAuthenticator.getRestrictedSubQueueIdentifiers().isEmpty())
    }

    /**
     * Ensure that [MultiQueueAuthenticator.getRestrictedSubQueueIdentifiers] will retrieve the restrict sub-queue
     * identifiers even when new entries are added/removed and cleared.
     */
    @Test
    fun testGetRestrictedSubQueueIdentifiers()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        val subQueues = listOf("testGetRestrictedSubQueueIdentifiers1", "testGetRestrictedSubQueueIdentifiers2",
            "testGetRestrictedSubQueueIdentifiers3", "testGetRestrictedSubQueueIdentifiers4",
            "testGetRestrictedSubQueueIdentifiers5", "testGetRestrictedSubQueueIdentifiers6")

        subQueues.forEach { subQueue -> Assertions.assertFalse(multiQueueAuthenticator.isRestricted(subQueue)) }
        subQueues.forEach { subQueue -> Assertions.assertTrue(multiQueueAuthenticator.addRestrictedEntry(subQueue)) }
        subQueues.forEach { subQueue -> Assertions.assertTrue(multiQueueAuthenticator.isRestricted(subQueue)) }

        val restrictedIdentifiers = multiQueueAuthenticator.getRestrictedSubQueueIdentifiers()
        restrictedIdentifiers.forEach { identifier -> Assertions.assertTrue(multiQueueAuthenticator.isRestricted(identifier)) }

        Assertions.assertEquals(subQueues.size, restrictedIdentifiers.size)

        Assertions.assertTrue(multiQueueAuthenticator.removeRestriction(restrictedIdentifiers.elementAt(0)))
        val updatedIdentifiers = multiQueueAuthenticator.getRestrictedSubQueueIdentifiers()
        Assertions.assertEquals(restrictedIdentifiers.size - 1, updatedIdentifiers.size)
        Assertions.assertFalse(updatedIdentifiers.contains(restrictedIdentifiers.elementAt(0)))

        multiQueueAuthenticator.clearRestrictedSubQueues()
        val emptyIdentifiers = multiQueueAuthenticator.getRestrictedSubQueueIdentifiers()
        Assertions.assertTrue(emptyIdentifiers.isEmpty())
    }

    /**
     * Ensure [MultiQueueAuthenticator.canAccessSubQueue] returns `false` or throws a [MultiQueueAuthorisationException]
     * if a reserved sub-queue name is used.
     */
    @Test
    fun testCanAccessSubQueue_UsingReservedSubQueue()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(multiQueueAuthenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, multiQueueAuthenticator.getRestrictionMode())

        if (multiQueueAuthenticator.getReservedSubQueues().isNotEmpty())
        {
            Assertions.assertFalse(multiQueueAuthenticator.canAccessSubQueue(multiQueueAuthenticator.getReservedSubQueues().first(), false))
            Assertions.assertThrows(MultiQueueAuthorisationException::class.java, {
                multiQueueAuthenticator.canAccessSubQueue(multiQueueAuthenticator.getReservedSubQueues().first(), true)
            })
        }
    }
}
