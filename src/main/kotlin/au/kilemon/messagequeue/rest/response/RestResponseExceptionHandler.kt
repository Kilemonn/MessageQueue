package au.kilemon.messagequeue.rest.response

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
}
