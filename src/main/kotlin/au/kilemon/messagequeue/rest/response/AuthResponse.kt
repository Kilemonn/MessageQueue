package au.kilemon.messagequeue.rest.response

import au.kilemon.messagequeue.filter.CorrelationIdFilter
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.MDC

/**
 * A response object which wraps the response jwt token.
 *
 * @author github.com/Kilemonn
 */
@JsonPropertyOrder("correlationId", "subQueue", "token")
class AuthResponse
{
    @Schema(title = "Request correlation ID", example = "1599dcd3-7424-4f97-bc99-b9b3e5c53d59",
        description = "A UUID that uniquely identifies the performed request. This will correlate with any logs written as part of this request for debugging purposes.")
    val correlationId: String? = MDC.get(CorrelationIdFilter.CORRELATION_ID)

    @Schema(title = "Access Token", example = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJraWxlbW9uL21lc3NhZ2UtcXVldWUiLCJTdWItUXVldWUtSWRlbnRpZmllciI6InRlc3RSZXN0cmljdFN1YlF1ZXVlX3Rva2VuR2VuZXJhdGVkIn0.Vr18hd41P0Vp7TOcatWrfBQOQZTEYZx2RAY7A82uwsUeSZfITPpSb3V96YN9IhuAoYWyj2GJRfWvlFLvokNzIQ",
        description = "The created access token that is required when performing future interactions with the returned sub-queue.")
    val token: String

    @Schema(title = "Sub-queue identifier", example = "My-Queue",
        description = "The unique sub-queue identifier which indicates which sub-queue that the returned token must be provided to when performing future interactions.")
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
