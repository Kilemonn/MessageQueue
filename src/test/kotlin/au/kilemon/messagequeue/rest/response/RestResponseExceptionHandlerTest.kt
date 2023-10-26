package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthenticationException
import au.kilemon.messagequeue.filter.CorrelationIdFilter
import org.junit.jupiter.api.AfterEach
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
class RestResponseExceptionHandlerTest
{
    private val responseHandler = RestResponseExceptionHandler()

    @BeforeEach
    fun setUp()
    {
        MDC.clear()
    }

    /**
     * This should be after class but works better here.
     */
    @AfterEach
    fun tearDown()
    {
        MDC.clear()
    }

    /**
     * Ensure [RestResponseExceptionHandler#handleResponseStatusException] sets all the properties required to create an
     * [ErrorResponse] are correctly extracted from the [ResponseStatusException].
     */
    @Test
    fun testHandleResponseStatusException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)
        val message = "Bad error message"
        val statusCode = HttpStatus.I_AM_A_TEAPOT
        val exception = ResponseStatusException(statusCode, message)
        val response = responseHandler.handleResponseStatusException(exception)

        Assertions.assertEquals(statusCode, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertEquals(message, response.body!!.message)
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }

    /**
     * Ensure the [RestResponseExceptionHandler#handleMultiQueueAuthenticationException] returns the appropriate
     * response code and message on error.
     */
    @Test
    fun testHandleMultiQueueAuthenticationException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)
        val message = "testHandleMultiQueueAuthenticationException"
        val exception = MultiQueueAuthenticationException(message)
        val response = responseHandler.handleMultiQueueAuthenticationException(exception)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertEquals(String.format(MultiQueueAuthenticationException.MESSAGE_FORMAT, message), response.body!!.message)
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }
}
