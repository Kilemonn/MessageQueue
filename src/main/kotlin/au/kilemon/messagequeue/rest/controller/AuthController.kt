package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.token.JwtTokenProvider
import au.kilemon.messagequeue.filter.JwtAuthenticationFilter
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.AuthResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.Generated
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = AuthController.AUTH_TAG)
@RestController
@RequestMapping(AuthController.AUTH_PATH)
open class AuthController : HasLogger
{
    companion object
    {
        /**
         * The [Tag] for the [AuthController] endpoints.
         */
        const val AUTH_TAG: String = "Auth"

        /**
         * The base path for the [AuthController].
         */
        const val AUTH_PATH = "/auth"
    }

    override val LOG: Logger = this.initialiseLogger()

    @Autowired
    @get:Generated
    @set:Generated
    lateinit var multiQueueAuthenticator: MultiQueueAuthenticator

    @Autowired
    @get:Generated
    @set:Generated
    lateinit var multiQueue: MultiQueue

    @Autowired
    @get:Generated
    @set:Generated
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Operation(summary = "Create restriction on sub-queue.", description = "Create restriction a specific sub-queue to require authentication for future interactions and retrieve a token used to interact with this sub-queue.")
    @PostMapping("/{${RestParameters.QUEUE_TYPE}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully registered the sub-queue identifier and returns an appropriate token for future access to the sub-queue."),
        ApiResponse(responseCode = "204", description = "The MultiQueue is in a no-auth mode and tokens cannot be generated.", content = [Content()]), // Add empty Content() to remove duplicate responses in swagger docsApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()])
        ApiResponse(responseCode = "409", description = "A sub-queue with the provided identifier is already authorised.", content = [Content()]),
        ApiResponse(responseCode = "500", description = "There was an error generating the auth token for the sub-queue.", content = [Content()])
    )
    fun restrictSubQueue(@Parameter(`in` = ParameterIn.PATH, required = true, description = "")
                         @PathVariable(required = true, name = RestParameters.QUEUE_TYPE) queueType: String,
                          @Parameter(`in` = ParameterIn.QUERY, required = false, description = "The generated token's expiry in minutes.")
                          @RequestParam(required = false, name = RestParameters.EXPIRY) expiry: Long?): ResponseEntity<AuthResponse>
    {
        if (multiQueueAuthenticator.isInNoneMode())
        {
            LOG.trace("Requested token for sub-queue [{}] is not provided as queue is in mode [{}].", queueType,
                multiQueueAuthenticator.getAuthenticationType())
            return ResponseEntity.noContent().build()
        }

        if (multiQueueAuthenticator.isRestricted(queueType))
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
        else
        {
            val wasAdded = multiQueueAuthenticator.addRestrictedEntry(queueType)
            val token = jwtTokenProvider.createTokenForSubQueue(queueType, expiry)
            return if (token.isEmpty || !wasAdded)
            {
                LOG.error("Failed to generated token for sub-queue [{}].", queueType)
                ResponseEntity.internalServerError().build()
            }
            else
            {
                LOG.info("Successfully generated token for sub-queue [{}] with expiry [{}] minutes.", queueType, expiry)
                ResponseEntity.ok(AuthResponse(token.get(), queueType))
            }
        }
    }

    @Operation(summary = "Remove restriction from sub-queue.", description = "Remove restriction from sub-queue so it can be accessed without restriction.")
    @DeleteMapping("/{${RestParameters.QUEUE_TYPE}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully removed restriction for the sub-queue identifier."),
        ApiResponse(responseCode = "204", description = "The MultiQueue is in a no-auth mode and sub-queue restrictions are disabled.", content = [Content()]), // Add empty Content() to remove duplicate responses in swagger docsApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()])
        ApiResponse(responseCode = "403", description = "Invalid token provided to remove restriction from requested sub-queue.", content = [Content()]),
        ApiResponse(responseCode = "404", description = "The requested sub-queue is not currently restricted.", content = [Content()]),
        ApiResponse(responseCode = "500", description = "There was an error releasing restriction from the sub-queue.", content = [Content()])
    )
    fun removeRestrictionFromSubQueue(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The sub-queue identifier to remove restriction for.") @PathVariable(required = true, name = RestParameters.QUEUE_TYPE) queueType: String,
                                      @Parameter(`in` = ParameterIn.QUERY, required = false, description = "If restriction is removed successfully indicate whether the sub-queue should be cleared now that it is accessible without a token.") @RequestParam(required = false, name = RestParameters.CLEAR_QUEUE) clearQueue: Boolean?): ResponseEntity<Void>
    {
        if (multiQueueAuthenticator.isInNoneMode())
        {
            LOG.trace("Requested to release authentication for sub-queue [{}] but queue is in mode [{}].", queueType,
                multiQueueAuthenticator.getAuthenticationType())
            return ResponseEntity.noContent().build()
        }

        val authedToken = JwtAuthenticationFilter.getSubQueue()
        if (authedToken == queueType)
        {
            if (multiQueueAuthenticator.isRestricted(queueType))
            {
                return if (multiQueueAuthenticator.removeRestriction(queueType))
                {
                    if (clearQueue == true)
                    {
                        LOG.info("Restriction removed and clearing sub-queue [{}].", queueType)
                        multiQueue.clearForType(queueType)
                    }
                    else
                    {
                        LOG.info("Removed restriction from sub-queue [{}] without clearing stored messages.", queueType)
                    }
                    ResponseEntity.ok().build()
                }
                else
                {
                    LOG.error("Failed to remove restriction for sub-queue [{}].", queueType)
                    ResponseEntity.internalServerError().build()
                }
            }
            else
            {
                LOG.info("Cannot remove restriction from a sub-queue [{}] that is not restricted.", queueType)
                return ResponseEntity.notFound().build()
            }
        }
        else
        {
            LOG.error("Failed to release authentication for sub-queue [{}] since provided token [{}] is not for the requested sub-queue.", queueType, authedToken)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }
}
