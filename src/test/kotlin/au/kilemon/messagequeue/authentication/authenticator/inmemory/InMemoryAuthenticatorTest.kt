package au.kilemon.messagequeue.authentication.authenticator.inmemory

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticatorTest
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.queue.MultiQueueTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * A test class for the [InMemoryAuthenticator] class.
 *
 * This class also does mock testing for the different types of [RestrictionMode] since they don't
 * rely on the backing mechanism.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Import( *[QueueConfiguration::class, LoggingConfiguration::class, MultiQueueTest.MultiQueueTestConfiguration::class] )
class InMemoryAuthenticatorTest: MultiQueueAuthenticatorTest()
{
    @BeforeEach
    fun setUp()
    {
        multiQueueAuthenticator.clearRestrictedSubQueues()
    }

    /**
     * Ensure that [MultiQueueAuthenticator.isInNoneMode] returns the correct value based on the stored
     * [RestrictionMode].
     */
    @Test
    fun testIsInNoneMode()
    {
        val authenticator = Mockito.spy(MultiQueueAuthenticator::class.java)
        Mockito.doReturn(RestrictionMode.NONE).`when`(authenticator).getRestrictionMode()
        Assertions.assertTrue(authenticator.isInNoneMode())

        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertFalse(authenticator.isInNoneMode())

        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(authenticator).getRestrictionMode()
        Assertions.assertFalse(authenticator.isInNoneMode())
    }

    /**
     * Ensure that [MultiQueueAuthenticator.isInHybridMode] returns the correct value based on the stored
     * [RestrictionMode].
     */
    @Test
    fun testIsInHybridMode()
    {
        val authenticator = Mockito.spy(MultiQueueAuthenticator::class.java)
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertTrue(authenticator.isInHybridMode())

        Mockito.doReturn(RestrictionMode.NONE).`when`(authenticator).getRestrictionMode()
        Assertions.assertFalse(authenticator.isInHybridMode())

        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(authenticator).getRestrictionMode()
        Assertions.assertFalse(authenticator.isInHybridMode())
    }

    /**
     * Ensure that [MultiQueueAuthenticator.isInRestrictedMode] returns the correct value based on the stored
     * [RestrictionMode].
     */
    @Test
    fun testIsInRestrictedMode()
    {
        val authenticator = Mockito.spy(MultiQueueAuthenticator::class.java)
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(authenticator).getRestrictionMode()
        Assertions.assertTrue(authenticator.isInRestrictedMode())

        Mockito.doReturn(RestrictionMode.NONE).`when`(authenticator).getRestrictionMode()
        Assertions.assertFalse(authenticator.isInRestrictedMode())

        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertFalse(authenticator.isInRestrictedMode())
    }
}
