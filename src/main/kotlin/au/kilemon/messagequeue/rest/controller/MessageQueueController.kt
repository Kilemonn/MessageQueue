package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.MessageResponse
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
 * @author github.com/KyleGonzalez
 */
@RestController
@RequestMapping(MessageQueueController.MESSAGE_QUEUE_BASE_PATH)
open class MessageQueueController
{
    companion object
    {
        /**
         * The base path for the [MessageQueueController].
         */
        const val MESSAGE_QUEUE_BASE_PATH: String = "/message/queue"

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
         * The resource path used to mark a message as `consumed` so that other calls know it is being handled or used already.
         */
        const val ENDPOINT_CONSUME: String = "/consume"

        /**
         * The resource path used to remove the `consumed` flag from a message in case the message needs to be processed again or is not correctly processed.
         */
        const val ENDPOINT_RELEASE: String = "/release"

        /**
         * The resource path used to retrieve the next `available` message in the queue for consumption.
         */
        const val ENDPOINT_NEXT: String = "/next"
    }

    @Autowired
    lateinit var messageQueue: MultiQueue

    @GetMapping("$ENDPOINT_TYPE/{queueType}",
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getValue(@PathVariable queueType: String?): ResponseEntity<String>
    {
        return if (queueType.isNullOrBlank())
        {
            ResponseEntity.ok(messageQueue.size.toString())
        }
        else
        {
            ResponseEntity.ok(messageQueue.getQueueForType(queueType).size.toString())
        }
    }

    /**
     * Get a message directly via [UUID] provided as a [String].
     *
     * @throws [HttpStatus.NOT_FOUND] if a [QueueMessage] with the provided [uuid] does not exist
     *
     * @param uuid the [UUID] of the message to retrieve
     * @return [MessageResponse] containing the found [QueueMessage] otherwise a [HttpStatus.NOT_FOUND] exception will be thrown
     */
    @GetMapping("$ENDPOINT_ENTRY/{uuid}",
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getEntry(@PathVariable uuid: String): ResponseEntity<MessageResponse>
    {
        val queueType = messageQueue.containsUUID(uuid)
        if (queueType.isPresent)
        {
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType.get())
            val entry = queueForType.stream().filter{ message -> message.uuid.toString() == uuid }.findFirst()
            if (entry.isPresent)
            {
                return ResponseEntity.ok(MessageResponse(entry.get()))
            }
        }

        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid.")
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
    @PostMapping(ENDPOINT_ENTRY,
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createMessage(@Valid @RequestBody queueMessage: QueueMessage): ResponseEntity<MessageResponse>
    {
        if (messageQueue.containsUUID(queueMessage.uuid.toString()).isPresent)
        {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Failed to add entry with UUID: ${queueMessage.uuid}, an entry with the same UUID already exists.")
        }

        val wasAdded = messageQueue.add(queueMessage)
        if (wasAdded)
        {
            return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse(queueMessage))
        }
        else
        {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add entry with UUID: ${queueMessage.uuid}")
        }
    }

    /**
     * A [GetMapping] which returns a list of all the `QueueTypes` defined in the [MultiQueue].
     *
     * @param includeEmpty to include `keys` which one had elements stored against them but don't at the moment.
     * @return a [Set] of [String] `queueType`s
     */
    @GetMapping(ENDPOINT_KEYS,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getKeys(@RequestParam(required = false) includeEmpty: Boolean?): ResponseEntity<Set<String>>
    {
        return ResponseEntity.ok(messageQueue.keys(includeEmpty ?: true))
    }

    /**
     * A [GetMapping] endpoint which retrieves all the stored [QueueMessage]s that are currently available in the [MultiQueue].
     *
     * @param detailed *true* if you require detailed information about each message and their payload/owner, otherwise **false** which displayed only limited information about each message
     * @param queueType the `type` to include, if provided only messages in this `queueType` will be retrieved.
     * @return a [Map] where the `key` is the `queueType` and the `value` is a comma separated list of all the [QueueMessage.toDetailedString]
     */
    @GetMapping(ENDPOINT_ALL,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(@RequestParam(required = false) detailed: Boolean?, @RequestParam(required = false) queueType: String?): ResponseEntity<Map<String, String>>
    {
        val responseMap = HashMap<String, String>()
        if ( !queueType.isNullOrBlank())
        {
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
            val queueDetails = queueForType.stream().map { message -> message.toDetailedString(detailed) }.collect(Collectors.toList())
            responseMap[queueType] = queueDetails.toString()
        }
        else
        {
            for (key: String in messageQueue.keys(false))
            {
                // No need to empty check since we passed `false` to `keys()` above
                val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(key)
                val queueDetails = queueForType.stream().map { message -> message.toDetailedString(detailed) }.collect(Collectors.toList())
                responseMap[key] = queueDetails.toString()
            }
        }
        return ResponseEntity.ok(responseMap)
    }

    /**
     * Retrieve all owned [QueueMessage] based on the provided user identifier.
     *
     * @param consumedBy the identifier used to indicate the owner of the [QueueMessage]s to return
     * @param queueType the `queueType` to search for the related [QueueMessage] owned by [consumedBy]
     * @return a [List] of [QueueMessage] based on messages that are `consumed` by [consumedBy] in the `queue` mapped to [queueType]
     */
    @GetMapping(ENDPOINT_OWNED,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOwned(@RequestParam consumedBy: String, @RequestParam queueType: String): ResponseEntity<List<MessageResponse>>
    {
        val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
        val ownedMessages =  queueForType.stream().filter { message -> message.consumed && message.consumedBy == consumedBy }.map { message -> MessageResponse(message) }.collect(Collectors.toList())
        return ResponseEntity.ok(ownedMessages)
    }

    /**
     * Mark as [QueueMessage] as `consumed` meaning that no other user is able to consume the [QueueMessage] while its in this state.
     * Only a `non-consumed` [QueueMessage] can be marked as `consumed` successfully.
     *
     * @throws [HttpStatus.NOT_FOUND] if a [QueueMessage] with the provided [uuid] does not exist
     * @throws [HttpStatus.CONFLICT] if the [QueueMessage] is already consumed by another user
     *
     * @param uuid the [UUID] of the [QueueMessage] to consume
     * @param consumedBy the identifier of the user who will consume the [QueueMessage]
     * @return the [QueueMessage] object after it has been marked as `consumed`. Returns [HttpStatus.ACCEPTED] if the [QueueMessage] is already consumed by the current user, otherwise [HttpStatus.OK] if it was not `consumed` previously.
     */
    @PutMapping(ENDPOINT_CONSUME,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun consumeMessage(@RequestParam uuid: String, @RequestParam consumedBy: String): ResponseEntity<MessageResponse>
    {
        val queueType = messageQueue.containsUUID(uuid)
        if (queueType.isPresent)
        {
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType.get())
            val message = queueForType.stream().filter { message -> message.uuid.toString() == uuid }.findFirst()
            if (message.isPresent)
            {
                val messageToRelease = message.get()
                if (messageToRelease.consumed)
                {
                    if (messageToRelease.consumedBy == consumedBy)
                    {
                        // The message is already in this state, returning 202 to tell the client that it is accepted but no action was done
                        return ResponseEntity.accepted().body(MessageResponse(messageToRelease))
                    }
                    else
                    {
                        throw ResponseStatusException(HttpStatus.CONFLICT, "The message with UUID: $uuid and $queueType is already held by instance with ID ${messageToRelease.consumedBy}.")
                    }
                }

                messageToRelease.consumedBy = consumedBy
                messageToRelease.consumed = true
                return ResponseEntity.ok(MessageResponse(messageToRelease))
            }
        }

        // No entries match the provided UUID (and queue type)
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid. (Queue-type: $queueType).")
    }

    /**
     * Retrieve the next `non-consumed` message in the [MultiQueue] for the provided [queueType] and assign it to the provided identifier [consumedBy].
     *
     * @param queueType the sub queue that the next [QueueMessage] should be from
     * @param consumedBy the identifier that the next [QueueMessage] should be `consumed` by before being returned
     * @return the next [QueueMessage] that is not `consumed` for the provided [queueType]. If none exist then [HttpStatus.NO_CONTENT] will be returned indicating that the queue is either empty or has no available [QueueMessage]s to consume.
     */
    @PutMapping(ENDPOINT_NEXT,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.OK)
    fun getNext(@RequestParam queueType: String, @RequestParam consumedBy: String): ResponseEntity<MessageResponse>
    {
        val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
        val nextMessage = queueForType.stream().filter { message -> !message.consumed }.findFirst()
        return if (nextMessage.isPresent)
        {
            val nextUnconsumedMessage = nextMessage.get()
            nextUnconsumedMessage.consumed = true
            nextUnconsumedMessage.consumedBy = consumedBy
            ResponseEntity.ok(MessageResponse(nextUnconsumedMessage))
        }
        else
        {
            ResponseEntity.noContent().build()
        }
    }

    /**
     * Release a `consumed` [QueueMessage] so that other users are able to consume the [QueueMessage].
     * Only a `consumed` [QueueMessage] can be `released` successfully.
     *
     * @throws [HttpStatus.NOT_FOUND] if a [QueueMessage] with the provided [uuid] does not exist
     * @throws [HttpStatus.CONFLICT] if the [QueueMessage] is consumed by another user
     *
     * @param uuid the [UUID] of the [QueueMessage] to release
     * @param consumedBy the identifier of the user who **SHOULD** currently hold this message, if this user does not hold this message a [HttpStatus.CONFLICT] will be thrown
     * @return the [QueueMessage] object after it has been `released`. Returns [HttpStatus.ACCEPTED] if the [QueueMessage] is already `released`, otherwise [HttpStatus.OK] if it was `released` successfully.
     */
    @PutMapping(ENDPOINT_RELEASE,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun releaseMessage(@RequestParam uuid: String, @RequestParam(required = false) consumedBy: String?)
    {
        val queueType = messageQueue.containsUUID(uuid)
        if (queueType.isPresent)
        {
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType.get())
            val message = queueForType.stream().filter { message -> message.uuid.toString() == uuid }.findFirst()
            if (message.isPresent)
            {
                val messageToRelease = message.get()
                if ( !messageToRelease.consumed)
                {
                    // The message is already in this state, returning 202 to tell the client that it is accepted but no action was done
                    ResponseEntity.accepted().body(messageToRelease)
                    return
                }

                if (consumedBy.isNullOrBlank() && messageToRelease.consumedBy != consumedBy)
                {
                    throw ResponseStatusException(HttpStatus.CONFLICT, "The message with UUID: $uuid and $queueType cannot be released because it is already held by instance with ID ${messageToRelease.consumedBy} and a provided ID was $consumedBy.")
                }
                messageToRelease.consumedBy = null
                messageToRelease.consumed = false
                ResponseEntity.ok(messageToRelease)
            }
        }

        // No entries match the provided UUID (and queue type)
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid. (Queue-type: $queueType).")
    }

    /**
     * Removes a [QueueMessage] from the [MultiQueue].
     * If a [consumedBy] identifier is provided then the found [QueueMessage] matching the provided [UUID] must also be `consumedBy` this identifier. Otherwise, if not provided the matching message will be removed.
     *
     * @throws [HttpStatus.NOT_FOUND] if a [QueueMessage] with the provided [uuid] does not exist
     * @throws [HttpStatus.FORBIDDEN] if the found [QueueMessage] is `consumed` but the [QueueMessage.consumedBy] does match the [consumedBy]
     *
     * @param uuid the [UUID] of the [QueueMessage] to remove
     * @param consumedBy the identifier of the user who **SHOULD** currently have the [QueueMessage] `consumed`, otherwise `null` if you want to force remove it
     * @return [HttpStatus.NO_CONTENT]
     */
    @DeleteMapping
    fun removeMessage(@RequestParam uuid: String, @RequestParam(required = false) consumedBy: String?)
    {
        val queueType = messageQueue.containsUUID(uuid)
        if (queueType.isPresent)
        {
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType.get())
            val message = queueForType.stream().filter { message -> message.uuid.toString() == uuid }.findFirst()
            if (message.isPresent)
            {
                val messageToRemove = message.get()
                if ( !consumedBy.isNullOrBlank() && messageToRemove.consumed && messageToRemove.consumedBy != consumedBy)
                {
                    throw ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to remove message with UUID $uuid in Queue $queueType because the provided consumedBy: $consumedBy does not match the message's consumedBy: ${messageToRemove.consumedBy}")
                }
                else
                {
                    queueForType.remove(messageToRemove)
                }
                ResponseEntity.noContent()
            }
        }

        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid. (Queue-type: $queueType).")
    }
}
