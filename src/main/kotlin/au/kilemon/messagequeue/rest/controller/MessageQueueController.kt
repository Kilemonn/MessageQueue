package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.logging.HasLogger
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.queue.exception.DuplicateMessageException
import au.kilemon.messagequeue.queue.exception.HealthCheckFailureException
import au.kilemon.messagequeue.rest.response.MessageResponse
import io.swagger.v3.oas.annotations.Hidden
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
import org.springframework.web.server.ResponseStatusException
import java.util.*
import java.util.stream.Collectors
import javax.validation.Valid

/**
 * The REST controller for the [MultiQueue]. It exposes endpoints to access and manipulate the queue and the messages inside it.
 * Using a base path of [MessageQueueController.MESSAGE_QUEUE_BASE_PATH] and various endpoints to manage the [MultiQueue].
 *
 * @author github.com/Kilemonn
 */
@Tag(name = MessageQueueController.MESSAGE_QUEUE_TAG)
@RestController
@RequestMapping(MessageQueueController.MESSAGE_QUEUE_BASE_PATH)
open class MessageQueueController : HasLogger
{
    override val LOG: Logger = initialiseLogger()

    companion object
    {
        /**
         * The [Tag] for the [MessageQueueController] endpoints.
         */
        const val MESSAGE_QUEUE_TAG: String = "Message Queue"

        /**
         * The base path for the [MessageQueueController].
         */
        const val MESSAGE_QUEUE_BASE_PATH: String = "/queue"

        /**
         * The resource path used to retrieve all messages stored in the [MultiQueue].
         */
        const val ENDPOINT_ALL: String = "/all"

        /**
         * The resource path used to create and get a message.
         */
        const val ENDPOINT_ENTRY: String = "/entry"

        /**
         * The resource path used to view messages that are within the same `sub queue`.
         */
        const val ENDPOINT_TYPE: String = "/type"

        /**
         * The resource path used to get all messages owned by a specific identifier.
         */
        const val ENDPOINT_OWNED: String = "/owned"

        /**
         * The resource path used to get all of the `keys` stored within the [MultiQueue] map.
         */
        const val ENDPOINT_KEYS: String = "/keys"

        /**
         * The resource path used to mark a message as `assigned` so that other calls know it is being handled or used already.
         */
        const val ENDPOINT_ASSIGN: String = "/assign"

        /**
         * The resource path used to remove the `assigned` flag from a message in case the message needs to be processed again or is not correctly processed.
         */
        const val ENDPOINT_RELEASE: String = "/release"

        /**
         * The resource path used to retrieve the next `available` message in the queue for consumption.
         */
        const val ENDPOINT_NEXT: String = "/next"

        /**
         * The resource path used to retrieve the different owners within the multi-queue or sub-queue.
         */
        const val ENDPOINT_OWNERS: String = "/owners"

        /**
         * A resource used to check system health.
         */
        const val ENDPOINT_HEALTH_CHECK: String = "/healthcheck"
    }

    @Autowired
    private lateinit var messageQueue: MultiQueue

    @Autowired
    private lateinit var authenticator: MultiQueueAuthenticator

    /**
     * Retrieve information about the whole [MultiQueue]. Specifically data related information.
     */
    @Hidden
    @Operation(summary = "Retrieve queue information for the whole multi queue.", description = "Retrieve information about the whole queue, specifically information on the queue entries.")
    @GetMapping(ENDPOINT_TYPE, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "200", description = "Successfully returns the information payload.")
    fun getAllQueueTypeInfo(): ResponseEntity<String>
    {
        val size = messageQueue.size
        LOG.debug("Returning total multi-queue size [{}].", size)
        return ResponseEntity.ok(size.toString())
    }

    /**
     * Retrieve information about a specific queue within [MultiQueue], based on the provided `queueType`. Specifically data related information.
     */
    @Hidden
    @Operation(summary = "Retrieve queue information for a specific sub queue.", description = "Retrieve information about the specified queueType within the queue, specifically information on the queue entries.")
    @GetMapping("$ENDPOINT_TYPE/{${RestParameters.QUEUE_TYPE}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "200", description = "Successfully returns the information payload.")
    fun getQueueTypeInfo(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The queueType to retrieve information about.")
                         @PathVariable(name = RestParameters.QUEUE_TYPE) queueType: String): ResponseEntity<String>
    {
        authenticator.canAccessSubQueue(queueType)

        val queueForType = messageQueue.getQueueForType(queueType)
        LOG.debug("Returning size [{}] for queue with type [{}].", queueForType.size, queueType)
        return ResponseEntity.ok(queueForType.size.toString())
    }

    /**
     * An endpoint to perform health checks on the underlying storage medium.
     * This call is delegated to the [MultiQueue.performHealthCheck] and returns [HttpStatus.OK] if the check is successful, otherwise [HttpStatus.INTERNAL_SERVER_ERROR] is returned.
     */
    @Operation(summary = "Perform health check to verify application status.", description = "Request the application to perform a health check to verify operational status.")
    @GetMapping(ENDPOINT_HEALTH_CHECK)
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Application health check has passed"),
        ApiResponse(responseCode = "500", description = "There was an error performing the health check, the application is not in an operational state.")
    )
    fun getHealthCheck(): ResponseEntity<Void>
    {
        return try
        {
            messageQueue.performHealthCheck()
            LOG.trace("Health check passed.")
            ResponseEntity.ok().build()
        }
        catch(ex: HealthCheckFailureException)
        {
            LOG.error("Health check failed.", ex)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * Get a message directly via [UUID] provided as a [String].
     *
     * @throws [HttpStatus.NO_CONTENT] if a [QueueMessage] with the provided [uuid] does not exist
     *
     * @param uuid the [UUID] of the message to retrieve
     * @return [MessageResponse] containing the found [QueueMessage] otherwise a [HttpStatus.NO_CONTENT] exception will be thrown
     */
    @Operation(summary = "Retrieve a queue message by UUID.", description = "Retrieve a queue message regardless of its sub queue, directly by UUID.")
    @GetMapping("$ENDPOINT_ENTRY/{${RestParameters.UUID}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully returns the queue message matching the provided UUID."),
        ApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()]) // Add empty Content() to remove duplicate responses in swagger docs
    )
    fun getEntry(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The UUID of the queue message to retrieve.") @PathVariable(name = RestParameters.UUID) uuid: String): ResponseEntity<MessageResponse>
    {
        val entry = messageQueue.getMessageByUUID(uuid)
        if (entry.isPresent)
        {
            val foundEntry = entry.get()
            authenticator.canAccessSubQueue(foundEntry.type)
            LOG.debug("Found message with UUID [{}] in queue with type [{}].", foundEntry.uuid, foundEntry.type)
            return ResponseEntity.ok(MessageResponse(foundEntry))
        }

        LOG.debug("Failed to find entry with UUID [{}].", uuid)
        return ResponseEntity.noContent().build()
    }

    /**
     * Create a new [QueueMessage] with the provided [RequestBody].
     * The [QueueMessage] will not be created if a [QueueMessage] already exists with the same [QueueMessage.uuid].
     *
     * @throws [HttpStatus.INTERNAL_SERVER_ERROR] if there is an issue adding the new [QueueMessage]
     * @throws [HttpStatus.CONFLICT] if a [QueueMessage] already exists with the same [UUID]
     *
     * @param queueMessage the new [QueueMessage] to create.
     * @return the created [QueueMessage] wrapped in a [MessageResponse]
     */
    @Operation(summary = "Create a new queue message.", description = "Create a new queue message.")
    @PostMapping(ENDPOINT_ENTRY, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Successfully created the provided queue message."),
        ApiResponse(responseCode = "409", description = "A queue message already exists with the same UUID.", content = [Content()]), // Add empty Content() to remove duplicate responses in swagger docs
        ApiResponse(responseCode = "500", description = "An internal system error occurred when adding the new queue message.", content = [Content()])
    )
    fun createMessage(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "The new queue message to create in the multi queue.") @Valid @RequestBody queueMessage: QueueMessage): ResponseEntity<MessageResponse>
    {
        try
        {
            if (queueMessage.assignedTo != null && queueMessage.assignedTo!!.isBlank())
            {
                queueMessage.assignedTo = null
            }

            authenticator.canAccessSubQueue(queueMessage.type)

            val wasAdded = messageQueue.add(queueMessage)
            if (wasAdded)
            {
                LOG.debug("Added new message with UUID [{}] to queue with type [{}}.", queueMessage.uuid, queueMessage.type)
                return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse(queueMessage))
            }
            else
            {
                LOG.error("Failed to add entry with UUID [{}] to queue with type [{}]. AND the message does not already exist. This could be a memory limitation or an issue with the underlying collection.", queueMessage.uuid, queueMessage.type)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add entry with UUID: [${queueMessage.uuid}] to queue with type [${queueMessage.type}]")
            }
        }
        catch (ex: DuplicateMessageException)
        {
            val queueType = messageQueue.containsUUID(queueMessage.uuid).get()
            val errorMessage = "Failed to add entry with UUID [${queueMessage.uuid}], an entry with the same UUID already exists in queue with type [$queueType]."
            LOG.error(errorMessage)
            throw ResponseStatusException(HttpStatus.CONFLICT, errorMessage, ex)
        }
    }

    /**
     * A [GetMapping] which returns a list of all the `QueueTypes` defined in the [MultiQueue].
     *
     * @param includeEmpty to include `keys` which one had elements stored against them but don't at the moment. Default is `true`.
     * @return a [Set] of [String] `queueType`s
     */
    @Operation(summary = "Retrieve a list of all keys.", description = "Retrieve a list of all sub queue key values in the multi queue.")
    @GetMapping(ENDPOINT_KEYS, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "200", description = "Successfully returns the list of keys.")
    fun getKeys(@Parameter(`in` = ParameterIn.QUERY, required = false, description = "Indicates whether to include keys that currently have zero entries (but have had entries previously). Is true by default.")
                @RequestParam(required = false, name = RestParameters.INCLUDE_EMPTY) includeEmpty: Boolean?): ResponseEntity<Set<String>>
    {
        return ResponseEntity.ok(messageQueue.keys(includeEmpty ?: true))
    }

    @Operation(summary = "Delete a keys or all keys, in turn clearing that sub queue.", description = "Delete the sub queue that matches the provided key. If no key is provided, all sub queues will be cleared.")
    @DeleteMapping(ENDPOINT_KEYS, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "204", description = "Successfully cleared the sub queue(s) with the provided key, or all sub queues if the key is null.")
    fun deleteKeys(@Parameter(`in` = ParameterIn.QUERY, required = false, description = "The queue type to clear the sub queue of. If it is not provided, all sub queues will be cleared.")
                @RequestParam(required = false, name = RestParameters.QUEUE_TYPE) queueType: String?): ResponseEntity<Void>
    {
        if (queueType != null)
        {
            authenticator.canAccessSubQueue(queueType)
            messageQueue.clearForType(queueType)
        }
        else
        {
            messageQueue.clear()
        }
        return ResponseEntity.noContent().build()
    }

    /**
     * A [GetMapping] endpoint which retrieves all the stored [QueueMessage]s that are currently available in the [MultiQueue].
     *
     * @param detailed *true* if you require detailed information about each message and their payload/owner, otherwise **false** which displayed only limited information about each message
     * @param queueType the `type` to include, if provided only messages in this `queueType` will be retrieved.
     * @return a [Map] where the `key` is the `queueType` and the `value` is a comma separated list of all the [QueueMessage.removePayload]
     */
    @Operation(summary = "Retrieve a limited or full version of the held messages.", description = "Retrieve queue message summaries for the held messages. This can be limited to a specific sub queue type and complete message detail to be included in the response if requested.")
    @GetMapping(ENDPOINT_ALL, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "200", description = "Successfully returns the list of summary entries for either the whole multi-queue or the sub queue.")
    fun getAll(@Parameter(`in` = ParameterIn.QUERY, required = false, description = "Indicates whether the response messages should contain all message details including the underlying payload. By default details are hidden.") @RequestParam(required = false, name = RestParameters.DETAILED) detailed: Boolean = false,
               @Parameter(`in` = ParameterIn.QUERY, required = false, description = "The sub queue type to search, if not provide all messages in the whole multi-queue will be returned.") @RequestParam(required = false, name = RestParameters.QUEUE_TYPE) queueType: String?): ResponseEntity<Map<String, List<QueueMessage>>>
    {
        val responseMap = HashMap<String, List<QueueMessage>>()
        if ( !queueType.isNullOrBlank())
        {
            LOG.debug("Retrieving all entry details from queue with type [{}].", queueType)
            authenticator.canAccessSubQueue(queueType)
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
            val queueDetails = queueForType.stream().map { message -> message.removePayload(detailed) }.collect(Collectors.toList())
            responseMap[queueType] = queueDetails
        }
        else
        {
            LOG.debug("Retrieving all entry details from all queue types.")
            for (key: String in messageQueue.keys(false))
            {
                if (authenticator.canAccessSubQueue(key, false))
                {
                    // No need to empty check since we passed `false` to `keys()` above
                    val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(key)
                    val queueDetails = queueForType.stream().map { message -> message.removePayload(detailed) }.collect(Collectors.toList())
                    responseMap[key] = queueDetails
                }
            }
        }
        return ResponseEntity.ok(responseMap)
    }

    /**
     * Retrieve all owned [QueueMessage] based on the provided user identifier.
     *
     * @param assignedTo the identifier used to indicate the owner of the [QueueMessage]s to return
     * @param queueType the `queueType` to search for the related [QueueMessage] owned by [assignedTo]
     * @return a [List] of [QueueMessage] based on messages that are `assigned` to the [assignedTo] in the `queue` mapped to [queueType]
     */
    @Operation(summary = "Retrieve all owned queue messages based on the provided user identifier.", description = "Retrieve all owned messages for the provided assignee identifier for the provided sub queue type.")
    @GetMapping(ENDPOINT_OWNED, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "200", description = "Successfully returns the list of owned queue messages in the sub queue for the provided assignee identifier.")
    fun getOwned(@Parameter(`in` = ParameterIn.QUERY, required = true, description = "The identifier that must match the message's `assigned` property in order to be returned.") @RequestParam(required = true, name = RestParameters.ASSIGNED_TO) assignedTo: String,
                 @Parameter(`in` = ParameterIn.QUERY, required = true, description = "The sub queue to search for the assigned messages.") @RequestParam(required = true, name = RestParameters.QUEUE_TYPE) queueType: String): ResponseEntity<List<MessageResponse>>
    {
        authenticator.canAccessSubQueue(queueType)

        val assignedMessages: Queue<QueueMessage> = messageQueue.getAssignedMessagesForType(queueType, assignedTo)
        val ownedMessages = assignedMessages.stream().map { message -> MessageResponse(message) }.collect(Collectors.toList())
        LOG.debug("Found [{}] owned entries within queue with type [{}] for user with identifier [{}].", ownedMessages.size, queueType, assignedTo)
        return ResponseEntity.ok(ownedMessages)
    }

    /**
     * Mark as [QueueMessage] as `assigned` meaning that no other user is able to own the [QueueMessage] while its in this state.
     * Only a `non-assigned` [QueueMessage] can be marked as `assigned` successfully.
     *
     * @throws [HttpStatus.NO_CONTENT] if a [QueueMessage] with the provided [uuid] does not exist
     * @throws [HttpStatus.CONFLICT] if the [QueueMessage] is already assigned to another user
     *
     * @param uuid the [UUID] of the [QueueMessage] to assign
     * @param assignedTo the identifier of the user who will be assigned the [QueueMessage]
     * @return the [QueueMessage] object after it has been marked as `assigned`. Returns [HttpStatus.ACCEPTED] if the [QueueMessage] is already assigned to the current user, otherwise [HttpStatus.OK] if it was not `assigned` previously.
     */
    @Operation(summary = "Assign an existing queue message to the provided identifier.", description = "Assign an existing queue message to the provided identifier. The message must already exist and not be assigned already to another identifier in order to be successfully assigned to the provided identifier.")
    @PutMapping("$ENDPOINT_ASSIGN/{${RestParameters.UUID}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully assigned the message to the provided identifier. The message was not previously assigned."),
        ApiResponse(responseCode = "202", description = "The message was already assigned to the provided identifier.", content = [Content()]),
        ApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()]),
        ApiResponse(responseCode = "409", description = "The message is already assigned to another identifier.", content = [Content()])
    )
    fun assignMessage(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The queue message UUID to assign.") @PathVariable(required = true, name = RestParameters.UUID) uuid: String,
                      @Parameter(`in` = ParameterIn.QUERY, required = true, description = "The identifier to assign the queue message to.") @RequestParam(required = true, name = RestParameters.ASSIGNED_TO) assignedTo: String): ResponseEntity<MessageResponse>
    {
        val message = messageQueue.getMessageByUUID(uuid)
        if (message.isPresent)
        {
            val messageToAssign = message.get()
            authenticator.canAccessSubQueue(messageToAssign.type
            )
            if (!messageToAssign.assignedTo.isNullOrBlank())
            {
                if (messageToAssign.assignedTo == assignedTo)
                {
                    // The message is already in this state, returning 202 to tell the client that it is accepted but no action was done
                    LOG.debug("Message with uuid [{}] in queue with type [{}] is already assigned to the identifier [{}].", messageToAssign.uuid, messageToAssign.type, assignedTo)
                    return ResponseEntity.accepted().body(MessageResponse(messageToAssign))
                }
                else
                {
                    LOG.error("Message with uuid [{}] in queue with type [{}] is already assigned to the identifier [{}]. Attempting to assign to identifier [{}].", messageToAssign.uuid, messageToAssign.type, messageToAssign.assignedTo, assignedTo)
                    throw ResponseStatusException(HttpStatus.CONFLICT, "The message with UUID: [$uuid] and [${messageToAssign.type}] is already assigned to the identifier [${messageToAssign.assignedTo}].")
                }
            }

            messageToAssign.assignedTo = assignedTo
            messageQueue.persistMessage(messageToAssign)
            LOG.debug("Assigned message with UUID [{}] to identifier [{}].", messageToAssign.uuid, assignedTo)
            return ResponseEntity.ok(MessageResponse(messageToAssign))
        }

        // No entries match the provided UUID
        LOG.debug("Could not find message to assign with UUID [{}].", uuid)
        return ResponseEntity.noContent().build()
    }

    /**
     * Retrieve the next `non-assigned` message in the [MultiQueue] for the provided [queueType] and assign it to the provided identifier [assignedTo].
     *
     * @param queueType the sub queue that the next [QueueMessage] should be from
     * @param assignedTo the identifier that the next [QueueMessage] should be `assigned` to before being returned
     * @return the next [QueueMessage] that is not `assigned` in the provided [queueType]. If none exist then [HttpStatus.NO_CONTENT] will be returned indicating that the queue is either empty or has no available [QueueMessage]s to assign.
     */
    @Operation(summary = "Retrieve the next available unassigned message in the queue.", description = "Retrieve the next available message in the queue for the provided sub queue identifier that is not assigned. The message will be assigned to the provided identifier then returned.")
    @PutMapping(ENDPOINT_NEXT, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully returns the next message in queue after assigning it to the provided `assignedTo` identifier."),
        ApiResponse(responseCode = "204", description = "No messages are available.", content = [Content()])
    )
    fun getNext(@Parameter(`in` = ParameterIn.QUERY, required = true, description = "The sub queue identifier to query the next available message from.") @RequestParam(required = true, name = RestParameters.QUEUE_TYPE) queueType: String,
                @Parameter(`in` = ParameterIn.QUERY, required = true, description = "The identifier to assign the next available message to if one exists.") @RequestParam(required = true, name = RestParameters.ASSIGNED_TO) assignedTo: String): ResponseEntity<MessageResponse>
    {
        authenticator.canAccessSubQueue(queueType)

        val queueForType: Queue<QueueMessage> = messageQueue.getUnassignedMessagesForType(queueType)
        return if (queueForType.iterator().hasNext())
        {
            val nextUnassignedMessage = queueForType.iterator().next()
            LOG.debug("Retrieving and assigning next message for queue type [{}] with UUID [{}] to identifier [{}].", queueType, nextUnassignedMessage.uuid, assignedTo)
            nextUnassignedMessage.assignedTo = assignedTo
            messageQueue.persistMessage(nextUnassignedMessage)
            ResponseEntity.ok(MessageResponse(nextUnassignedMessage))
        }
        else
        {
            LOG.debug("No unassigned entries in queue with type [{}].", queueType)
            ResponseEntity.noContent().build()
        }
    }

    /**
     * Release an `assigned` [QueueMessage] so that other users are able be assigned the [QueueMessage].
     * Only an `assigned` [QueueMessage] can be `released` successfully.
     *
     * @throws [HttpStatus.NO_CONTENT] if a [QueueMessage] with the provided [uuid] does not exist
     * @throws [HttpStatus.CONFLICT] if the [QueueMessage] is assigned to another identifier
     *
     * @param uuid the [UUID] of the [QueueMessage] to release
     * @param assignedTo the identifier that **SHOULD** currently be assigned this message, if this identifier does not hold this message a [HttpStatus.CONFLICT] will be thrown
     * @return the [QueueMessage] object after it has been `released`. Returns [HttpStatus.ACCEPTED] if the [QueueMessage] is already `released`, otherwise [HttpStatus.OK] if it was `released` successfully.
     */
    @Operation(summary = "Release the message assigned to the provided identifier.", description = "Release an assigned message so it can be assigned to another identifier.")
    @PutMapping("$ENDPOINT_RELEASE/{${RestParameters.UUID}}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully released the message. The message was previously assigned."),
        ApiResponse(responseCode = "202", description = "The message is not currently assigned."),
        ApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()]),
        ApiResponse(responseCode = "409", description = "The identifier was provided and the message is assigned to another identifier so it cannot be released.", content = [Content()])
    )
    fun releaseMessage(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The UUID of the message to release.") @PathVariable(required = true, name = RestParameters.UUID) uuid: String,
                       @Parameter(`in` = ParameterIn.QUERY, required = false, description = "If provided, the message will only be released if the current assigned identifier matches this value.") @RequestParam(required = false, name = RestParameters.ASSIGNED_TO) assignedTo: String?): ResponseEntity<MessageResponse>
    {
        val message = messageQueue.getMessageByUUID(uuid)
        if (message.isPresent)
        {
            val messageToRelease = message.get()
            authenticator.canAccessSubQueue(messageToRelease.type)

            if (messageToRelease.assignedTo == null)
            {
                // The message is already in this state, returning 202 to tell the client that it is accepted but no action was done
                LOG.debug("Message with UUID [{}] is already released.", uuid)
                return ResponseEntity.accepted().body(MessageResponse(messageToRelease))
            }

            if (!assignedTo.isNullOrBlank() && messageToRelease.assignedTo != assignedTo)
            {
                val errorMessage = "The message with UUID: [$uuid] and [${messageToRelease.type}] cannot be released because it is already assigned to identifier [${messageToRelease.assignedTo}] and the provided identifier was [$assignedTo]."
                LOG.error(errorMessage)
                throw ResponseStatusException(HttpStatus.CONFLICT, errorMessage)
            }
            messageToRelease.assignedTo = null
            messageQueue.persistMessage(messageToRelease)
            LOG.debug("Released message with UUID [{}] on request from identifier [{}].", messageToRelease.uuid, assignedTo)
            return ResponseEntity.ok(MessageResponse(messageToRelease))
        }

        // No entries match the provided UUID (and queue type)
        LOG.debug("Could not find message to release with UUID [{}].", uuid)
        return ResponseEntity.noContent().build()
    }

    /**
     * Removes a [QueueMessage] from the [MultiQueue].
     * If an [assignedTo] identifier is provided then the found [QueueMessage] matching the provided [UUID] must also be [assignedTo] this same identifier.
     * Otherwise, if not provided the matching message will be removed regardless of the current assignee.
     *
     * @throws [HttpStatus.NO_CONTENT] if a [QueueMessage] with the provided [uuid] does not exist
     * @throws [HttpStatus.FORBIDDEN] if the found [QueueMessage] is `assigned` but the [QueueMessage.assignedTo] does match the [assignedTo]
     *
     * @param uuid the [UUID] of the [QueueMessage] to remove
     * @param assignedTo the identifier of the user who **SHOULD** currently have the [QueueMessage] `assigned` to them, otherwise `null` if you want to force remove it
     * @return [HttpStatus.NO_CONTENT]
     */
    @Operation(summary = "Remove a queue message by UUID.", description = "Remove a queue message by UUID. If `assignedTo` is provided, the message must be currently assigned to that identifier for it to be removed.")
    @DeleteMapping("$ENDPOINT_ENTRY/{${RestParameters.UUID}}")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully removed the message."),
        ApiResponse(responseCode = "204", description = "No queue messages match the provided UUID.", content = [Content()]),
        ApiResponse(responseCode = "403", description = "The provided identifier does not match the message's current assignee so it cannot be removed.", content = [Content()])
    )
    fun removeMessage(@Parameter(`in` = ParameterIn.PATH, required = true, description = "The UUID of the message to remove.") @PathVariable(required = true, name = RestParameters.UUID) uuid: String,
                      @Parameter(`in` = ParameterIn.QUERY, required = false, description = "If provided, the message will only be removed if it is assigned to an identifier that matches this value.") @RequestParam(required = false, name = RestParameters.ASSIGNED_TO) assignedTo: String?): ResponseEntity<Void>
    {
        val message = messageQueue.getMessageByUUID(uuid)
        if (message.isPresent)
        {
            val messageToRemove = message.get()
            authenticator.canAccessSubQueue(messageToRemove.type)
            if ( !assignedTo.isNullOrBlank() && messageToRemove.assignedTo != assignedTo)
            {
                val errorMessage = "Unable to remove message with UUID [$uuid] in Queue [${messageToRemove.type}] because the provided assignee identifier: [$assignedTo] does not match the message's assignee identifier: [${messageToRemove.assignedTo}]"
                LOG.error(errorMessage)
                throw ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage)
            }
            messageQueue.remove(messageToRemove)
            LOG.debug("Removed message with UUID [{}] on request from assignee [{}].", messageToRemove.uuid, assignedTo)
            return ResponseEntity.ok().build()
        }

        LOG.debug("Could not find message to remove with UUID [{}].", uuid)
        return ResponseEntity.noContent().build()
    }

    /**
     * Retrieve a map containing assignee identifiers mapped to a list of the sub-queues that they own messages in.
     * E.g.
     * ```
     * {
     *      "identifier1": ["queue-A", "queue-D", "queue-N"],
     *      "identifier2": ["queue-A", "queue-B", "queue-Z"]
     * }
     * ```
     * @param queueType the sub-queue identifier that you are interested in. If not provided, all sub-queues will be iterated through.
     * @return a [Map] of assignee identifiers mapped to a [Set] of the sub-queue identifiers that they have any assigned messages in.
     */
    @Operation(summary = "Retrieve all unique owner identifiers for either a specified sub-queue or all sub-queues.", description = "Retrieve all owner identifier mapped to a list of the sub-queue identifiers that they are assigned any messages in.")
    @GetMapping(ENDPOINT_OWNERS, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "200", description = "Successfully returns the map of owner identifiers mapped to all the sub-queues that they have one or more assigned messages in.")
    fun getOwners(@Parameter(`in` = ParameterIn.QUERY, required = false, description = "The sub queue to search for the owner identifiers.") @RequestParam(required = false, name = RestParameters.QUEUE_TYPE) queueType: String?): ResponseEntity<Map<String, HashSet<String>>>
    {
        if (queueType != null)
        {
            authenticator.canAccessSubQueue(queueType)
        }

        return ResponseEntity.ok(messageQueue.getOwnersAndKeysMap(queueType))
    }
}
