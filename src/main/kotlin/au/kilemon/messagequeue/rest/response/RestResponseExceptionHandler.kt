package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthenticationException
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthorisationException
import au.kilemon.messagequeue.queue.exception.IllegalSubQueueIdentifierException
import au.kilemon.messagequeue.queue.exception.MessageDeleteException
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * A response handler which maps the thrown [ResponseStatusException] into the [ErrorResponse].
 *
 * @author github.com/Kilemonn
 */
@ControllerAdvice
class RestResponseExceptionHandler: ResponseEntityExceptionHandler()
{
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse>
    {
        return ResponseEntity<ErrorResponse>(ErrorResponse(ex.reason), ex.statusCode)
    }

    @ExceptionHandler(MultiQueueAuthorisationException::class)
    fun handleMultiQueueAuthorisationException(ex: MultiQueueAuthorisationException): ResponseEntity<ErrorResponse>
    {
        return ResponseEntity<ErrorResponse>(ErrorResponse(ex.message), HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(MultiQueueAuthenticationException::class)
    fun handleMultiQueueAuthenticationException(ex: MultiQueueAuthenticationException): ResponseEntity<ErrorResponse>
    {
        return ResponseEntity<ErrorResponse>(ErrorResponse(ex.message), HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(IllegalSubQueueIdentifierException::class)
    fun handleIllegalSubQueueIdentifierException(ex: IllegalSubQueueIdentifierException): ResponseEntity<ErrorResponse>
    {
        return ResponseEntity<ErrorResponse>(ErrorResponse(ex.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MessageDeleteException::class)
    fun handleMessageDeleteException(ex: MessageDeleteException): ResponseEntity<ErrorResponse>
    {
        return ResponseEntity<ErrorResponse>(ErrorResponse(ex.message), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(MessageUpdateException::class)
    fun handleMessageUpdateException(ex: MessageUpdateException): ResponseEntity<ErrorResponse>
    {
        return ResponseEntity<ErrorResponse>(ErrorResponse(ex.message), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
