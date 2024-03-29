package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthenticationException
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthorisationException
import au.kilemon.messagequeue.filter.CorrelationIdFilter
import au.kilemon.messagequeue.queue.exception.IllegalSubQueueIdentifierException
import au.kilemon.messagequeue.queue.exception.MessageDeleteException
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
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
     * Ensure [RestResponseExceptionHandler.handleResponseStatusException] sets all the properties required to create an
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
     * Ensure the [RestResponseExceptionHandler.handleMultiQueueAuthorisationException] returns the appropriate
     * response code and message on error.
     */
    @Test
    fun testHandleMultiQueueAuthorisationException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)
        val message = "testHandleMultiQueueAuthorisationException"
        val exception = MultiQueueAuthorisationException(message, RestrictionMode.NONE)
        val response = responseHandler.handleMultiQueueAuthorisationException(exception)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertEquals(String.format(MultiQueueAuthorisationException.MESSAGE_FORMAT, message, RestrictionMode.NONE), response.body!!.message)
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }

    /**
     * Ensure the [RestResponseExceptionHandler.handleMultiQueueAuthenticationException] returns the appropriate
     * response code and message on error.
     */
    @Test
    fun testHandleMultiQueueAuthenticationException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)
        val exception = MultiQueueAuthenticationException()
        val response = responseHandler.handleMultiQueueAuthenticationException(exception)

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertEquals(MultiQueueAuthenticationException.ERROR_MESSAGE, response.body!!.message)
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }

    /**
     * Ensure the [RestResponseExceptionHandler.handleIllegalSubQueueIdentifierException] returns the appropriate response
     * and error message.
     */
    @Test
    fun testHandleIllegalSubQueueIdentifierException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)
        val subQueue = "testHandleIllegalSubQueueIdentifierException"
        val exception = IllegalSubQueueIdentifierException(subQueue)
        val response = responseHandler.handleIllegalSubQueueIdentifierException(exception)

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertTrue(response.body!!.message!!.contains(subQueue))
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }

    /**
     * Ensure the [RestResponseExceptionHandler.handleMessageUpdateException] returns the appropriate response
     * and error message.
     */
    @Test
    fun testHandleMessageUpdateException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)

        val uuid = UUID.randomUUID().toString()
        val exception = MessageUpdateException(uuid)
        val response = responseHandler.handleMessageUpdateException(exception)

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertTrue(response.body!!.message!!.contains(uuid))
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }

    /**
     * Ensure the [RestResponseExceptionHandler.handleMessageDeleteException] returns the appropriate response
     * and error message.
     */
    @Test
    fun testHandleMessageDeleteException()
    {
        val correlationId = UUID.randomUUID().toString()
        MDC.put(CorrelationIdFilter.CORRELATION_ID, correlationId)

        val uuid = UUID.randomUUID().toString()
        val exception = MessageDeleteException(uuid)
        val response = responseHandler.handleMessageDeleteException(exception)

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        Assertions.assertNotNull(response.body)
        Assertions.assertTrue(response.body!!.message!!.contains(uuid))
        Assertions.assertEquals(correlationId, response.body!!.correlationId)
    }
}
