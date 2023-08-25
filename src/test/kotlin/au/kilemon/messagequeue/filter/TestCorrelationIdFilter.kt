package au.kilemon.messagequeue.filter

import org.junit.AfterClass
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.*

/**
 * Test [CorrelationIdFilter] to make sure the [CorrelationIdFilter.CORRELATION_ID] is set correctly.
 *
 * @author github.com/Kilemonn
 */
class TestCorrelationIdFilter
{
    companion object
    {
        @JvmStatic
        @AfterClass
        fun tearDownClass()
        {
            MDC.clear()
        }
    }

    private val correlationIdFilter = CorrelationIdFilter()

    @BeforeEach
    fun setUp()
    {
        MDC.clear()
    }

    /**
     * Ensure the provided `correlationId` is used when [CorrelationIdFilter.setCorrelationId] is called with a non-null
     * argument.
     */
    @Test
    fun testSetCorrelationId_providedId()
    {
        val correlationId = "a-correlation-id-123456"
        Assertions.assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID))

        correlationIdFilter.setCorrelationId(correlationId)
        Assertions.assertEquals(correlationId, MDC.get(CorrelationIdFilter.CORRELATION_ID))
    }

    /**
     * Ensure that a [UUID] `correlationId` will be generated when [CorrelationIdFilter.setCorrelationId] is called
     * with a `null` argument.
     */
    @Test
    fun testSetCorrelationId_generatedId()
    {
        Assertions.assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID))
        correlationIdFilter.setCorrelationId(null)
        val generatedCorrelationId = MDC.get(CorrelationIdFilter.CORRELATION_ID)
        Assertions.assertEquals(UUID.fromString(generatedCorrelationId).toString(), generatedCorrelationId)
    }
}
