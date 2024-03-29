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
    private lateinit var multiQueueAuthenticator: MultiQueueAuthenticator

    @Autowired
    private lateinit var multiQueue: MultiQueue

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Operation(summary = "Get restricted sub-queue identifiers", description = "Get a list of the restricted sub-queue identifiers.")
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Returns a list of sub-queues marked as restricted that require a token to interact with."),
        ApiResponse(responseCode = "204", description = "The MultiQueue is in a no-auth mode and no sub-queues are marked as restricted.", content = [Content()])
    )
    fun getRestrictedSubQueueIdentifiers(): ResponseEntity<Set<String>>
    {
        if (multiQueueAuthenticator.isInNoneMode())
        {
            LOG.trace("Returning no restricted identifiers since the restriction mode is set to [{}].", multiQueueAuthenticator.getRestrictionMode())
            return ResponseEntity.noContent().build()
        }

        return ResponseEntity.ok(multiQueueAuthenticator.getRestrictedSubQueueIdentifiers())
    }

    @Operation(summary = "Create restriction on sub-queue.", description = "Create restriction a specific sub-queue to require authentication for future interactions and retrieve a token used to interact with this sub-queue.")
    @PostMapping("/{${RestParameters.SUB_QUEUE}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Successfully registered the sub-queue identifier and returns an appropriate token for future access to the sub-queue."),
        ApiResponse(responseCode = "204", description = "The MultiQueue is in a no-auth mode and tokens cannot be generated.", content = [Content()]), // Add empty Content() to remove duplicate responses in swagger docsApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()])
        ApiResponse(responseCode = "409", description = "A sub-queue with the provided identifier is already authorised.", content = [Content()]),
        ApiResponse(responseCode = "500", description = "There was an error generating the auth token for the sub-queue.", content = [Content()])
    )
    fun restrictSubQueue(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The sub-queue that you wish to restrict to allow further access only by callers that posses the returned token.")
                         @PathVariable(required = true, name = RestParameters.SUB_QUEUE) subQueue: String,
                         /*@Parameter(`in` = ParameterIn.QUERY, required = false, description = "The generated token's expiry in minutes.")
                         @RequestParam(required = false, name = RestParameters.EXPIRY) expiry: Long?*/): ResponseEntity<AuthResponse>
    {
        if (multiQueueAuthenticator.isInNoneMode())
        {
            LOG.trace("Requested token for sub-queue [{}] is not provided as queue is in mode [{}].", subQueue,
                multiQueueAuthenticator.getRestrictionMode())
            return ResponseEntity.noContent().build()
        }

        if (multiQueueAuthenticator.isRestricted(subQueue))
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
        else
        {
            // Generating the token first, so we don't need to roll back restriction add later if there is a problem
            val token = jwtTokenProvider.createTokenForSubQueue(subQueue, null)
            if (token.isEmpty)
            {
                LOG.error("Failed to generated token for sub-queue [{}].", subQueue)
                return ResponseEntity.internalServerError().build()
            }

            return if (!multiQueueAuthenticator.addRestrictedEntry(subQueue))
            {
                LOG.error("Failed to add restriction for sub-queue [{}].", subQueue)
                ResponseEntity.internalServerError().build()
            }
            else
            {
                LOG.info("Successfully generated token for sub-queue [{}].", subQueue)
                ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse(token.get(), subQueue))
            }
        }
    }

    @Operation(summary = "Remove restriction from sub-queue.", description = "Remove restriction from sub-queue so it can be accessed without restriction.")
    @DeleteMapping("/{${RestParameters.SUB_QUEUE}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully removed restriction for the sub-queue identifier."),
        ApiResponse(responseCode = "202", description = "The MultiQueue is in a no-auth mode and sub-queue restrictions are disabled.", content = [Content()]), // Add empty Content() to remove duplicate responses in swagger docsApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()])
        ApiResponse(responseCode = "204", description = "The requested sub-queue is not currently restricted.", content = [Content()]),
        ApiResponse(responseCode = "403", description = "Invalid token provided to remove restriction from requested sub-queue.", content = [Content()]),
        ApiResponse(responseCode = "500", description = "There was an error releasing restriction from the sub-queue.", content = [Content()])
    )
    fun removeRestrictionFromSubQueue(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The sub-queue identifier to remove restriction for.")
                                      @PathVariable(required = true, name = RestParameters.SUB_QUEUE) subQueue: String,
                                      @Parameter(`in` = ParameterIn.QUERY, required = false, description = "If restriction is removed successfully indicate whether the sub-queue should be cleared now that it is accessible without a token.")
                                      @RequestParam(required = false, name = RestParameters.CLEAR_QUEUE) clearQueue: Boolean?): ResponseEntity<Void>
    {
        if (multiQueueAuthenticator.isInNoneMode())
        {
            LOG.trace("Requested to release authentication for sub-queue [{}] but queue is in mode [{}].", subQueue,
                multiQueueAuthenticator.getRestrictionMode())
            return ResponseEntity.accepted().build()
        }

        val authedToken = JwtAuthenticationFilter.getSubQueue()
        if (authedToken == subQueue)
        {
            if (multiQueueAuthenticator.isRestricted(subQueue))
            {
                return if (multiQueueAuthenticator.removeRestriction(subQueue))
                {
                    if (clearQueue == true)
                    {
                        LOG.info("Restriction removed and clearing sub-queue [{}].", subQueue)
                        multiQueue.clearSubQueue(subQueue)
                    }
                    else
                    {
                        LOG.info("Removed restriction from sub-queue [{}] without clearing stored messages.", subQueue)
                    }
                    ResponseEntity.ok().build()
                }
                else
                {
                    LOG.error("Failed to remove restriction for sub-queue [{}].", subQueue)
                    ResponseEntity.internalServerError().build()
                }
            }
            else
            {
                LOG.info("Cannot remove restriction from a sub-queue [{}] that is not restricted.", subQueue)
                return ResponseEntity.noContent().build()
            }
        }
        else
        {
            LOG.error("Failed to release authentication for sub-queue [{}] since provided token [{}] is not for the requested sub-queue.", subQueue, authedToken)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }
}
