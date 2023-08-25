package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import org.junit.AfterClass
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.*

/**
 * Test [RestResponseExceptionHandler] to make sure the [ResponseStatusException] is correctly transformed to [ErrorResponse].
 *
 * @author github.com/Kilemonn
 */
class TestRestResponseExceptionHandler
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

    @BeforeEach
    fun setUp()
    {
        MDC.clear()
    }

    /**
     * Ensure all the properties required to create an [ErrorResponse] are correctly extracted from the [ResponseStatusException].
     */
    @Test
    fun testHandleResponseStatusException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)
        val responseHandler = RestResponseExceptionHandler()
        val message = "Bad error message"
        val statusCode = HttpStatus.FORBIDDEN
        val exception = ResponseStatusException(statusCode, message)
        val response = responseHandler.handleResponseStatusException(exception)

        Assertions.assertEquals(statusCode, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertEquals(message, response.body!!.message)
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }
}
