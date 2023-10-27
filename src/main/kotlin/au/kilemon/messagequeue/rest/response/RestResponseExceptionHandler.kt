package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthenticationException
import au.kilemon.messagequeue.authentication.exception.MultiQueueAuthorisationException
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
        return ResponseEntity<ErrorResponse>(ErrorResponse(ex.reason), ex.status)
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
}
