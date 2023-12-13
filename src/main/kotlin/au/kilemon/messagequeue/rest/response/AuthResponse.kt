package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.MDC
import java.util.Optional

/**
 * A response object which wraps the response jwt token.
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "subQueue", "token")
class AuthResponse
{
    @Schema(title = "The request correlation ID.", example = "1599dcd3-7424-4f97-bc99-b9b3e5c53d59")
    val correlationId: String? = Optional.ofNullable(MDC.get(CorrelationIdFilter.CORRELATION_ID)).orElse(null)

    @Schema(title = "The created access token that can be used to interact with the requested sub-queue.", example = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJraWxlbW9uL21lc3NhZ2UtcXVldWUiLCJTdWItUXVldWUtSWRlbnRpZmllciI6InRlc3RSZXN0cmljdFN1YlF1ZXVlX3Rva2VuR2VuZXJhdGVkIn0.Vr18hd41P0Vp7TOcatWrfBQOQZTEYZx2RAY7A82uwsUeSZfITPpSb3V96YN9IhuAoYWyj2GJRfWvlFLvokNzIQ")
    val token: String

    @Schema(title = "The sub-queue identifier that the returned token can access.", example = "My-Queue")
    val subQueue: String

    /**
     * Not converting to primary constructor, so we can use [Schema] annotations.
     */
    constructor(token: String, subQueue: String)
    {
        this.token = token
        this.subQueue = subQueue
    }
}
