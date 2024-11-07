package au.kilemon.messagequeue.filter

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.*

/**
 * Test [CorrelationIdFilter] to make sure the [CorrelationIdFilter.CORRELATION_ID] is set correctly.
 *
 * @author github.com/Kilemonn
 */
class CorrelationIdFilterTest
{
    private val correlationIdFilter = CorrelationIdFilter()

    @BeforeEach
    fun setUp()
    {
        MDC.clear()
    }

    /**
     * This should be afterclass but works better here.
     */
    @AfterEach
    fun tearDown()
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

    /**
     * Ensure that [CorrelationIdFilter.doFilter] will generate AND set the [CorrelationIdFilter.CORRELATION_ID] value
     * from the [MDC] into the [jakarta.servlet.ServletResponse].
     */
    @Test
    fun testDoFilter_withoutCorrelationIdInRequest()
    {
        val req = MockHttpServletRequest()
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        Assertions.assertNull(req.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
        correlationIdFilter.doFilter(req, res, chain)
        Assertions.assertNotNull(res.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
    }

    /**
     * Ensure that [CorrelationIdFilter.doFilter] will set the provided [CorrelationIdFilter.CORRELATION_ID]
     * into the response headers.
     */
    @Test
    fun testDoFilter_withCorrelationIdInRequest()
    {
        val req = MockHttpServletRequest()
        val res = MockHttpServletResponse()
        val chain = MockFilterChain()

        val uuid = UUID.randomUUID().toString()
        req.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, uuid)
        correlationIdFilter.doFilter(req, res, chain)

        Assertions.assertNotNull(res.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
        Assertions.assertEquals(uuid, res.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
    }
}
