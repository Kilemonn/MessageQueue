package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.EmptyResponse
import au.kilemon.messagequeue.rest.response.MessageResponse
import au.kilemon.messagequeue.rest.response.QueueMessageResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*
import java.util.stream.Collectors
import javax.validation.Valid
import javax.xml.ws.Response

/**
 * The REST controller for the [MultiQueue]. It exposes endpoints to access and manipulate the queue and the messages inside it.
 * Using a base path of [MessageQueueController.MESSAGE_QUEUE_BASE_PATH] and various endpoints to manage the [MultiQueue].
 *
 * @author github.com/KyleGonzalez
 */
@RestController
@RequestMapping(MessageQueueController.MESSAGE_QUEUE_BASE_PATH)
class MessageQueueController
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

    @GetMapping("$ENDPOINT_ENTRY/{uuid}",
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getEntry(@PathVariable uuid: String, @RequestParam(required = false) queueType: String?): ResponseEntity<MessageResponse>
    {
        if ( !queueType.isNullOrBlank())
        {
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
            val entry = queueForType.stream().filter{ message -> message.uuid.toString() == uuid }.findFirst()
            if (entry.isPresent)
            {
                return ResponseEntity.ok(QueueMessageResponse(entry.get()))
            }
        }
        else
        {
            for (key: String in messageQueue.keys(false))
            {
                val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(key)
                val entry = queueForType.stream().filter{ message -> message.uuid.toString() == uuid }.findFirst()
                if (entry.isPresent)
                {
                    return ResponseEntity.ok(QueueMessageResponse(entry.get()))
                }
            }
        }

        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid. (Queue-type: $queueType.")
    }

    @PostMapping(ENDPOINT_ENTRY,
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createMessage(@Valid @RequestBody queueMessage: QueueMessage): ResponseEntity<QueueMessageResponse>
    {
        val wasAdded = messageQueue.add(queueMessage)
        if (wasAdded)
        {
            return ResponseEntity.status(HttpStatus.CREATED).body(QueueMessageResponse(queueMessage))
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

    @GetMapping(ENDPOINT_OWNED,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOwned(@RequestParam consumedBy: String, @RequestParam queueType: String): ResponseEntity<List<MessageResponse>>
    {
        val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
        val ownedMessages =  queueForType.stream().filter { message -> message.consumed && message.consumedBy == consumedBy }.map { message -> QueueMessageResponse(message) }.collect(Collectors.toList())
        return ResponseEntity.ok(ownedMessages)
    }

    @PutMapping(ENDPOINT_CONSUME,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun consumeMessage(@RequestParam queueType: String, @RequestParam uuid: String, @RequestParam consumedBy: String)
    {
        val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
        val message = queueForType.stream().filter { message -> message.uuid.toString() == uuid }.findFirst()
        if (message.isPresent)
        {
            val messageToRelease = message.get()
            if (messageToRelease.consumed)
            {
                if (messageToRelease.consumedBy == consumedBy)
                {
                    // The message is already in this state, returning 202 to tell the client that it is accepted but no action was done
                    ResponseEntity.accepted().body(messageToRelease)
                    return
                }
                else
                {
                    throw ResponseStatusException(HttpStatus.CONFLICT, "The message with UUID: $uuid and $queueType is already held by instance with ID ${messageToRelease.consumedBy}.")
                }
            }

            messageToRelease.consumedBy = consumedBy
            messageToRelease.consumed = true
            ResponseEntity.ok(messageToRelease)
        }

        // No entries match the provided UUID (and queue type)
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid. (Queue-type: $queueType.")
    }

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
            ResponseEntity.ok(QueueMessageResponse(nextUnconsumedMessage))
        }
        else
        {
            ResponseEntity.ok(EmptyResponse())
        }
    }

    @PutMapping(ENDPOINT_RELEASE,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun releaseMessage(@RequestParam uuid: String, @RequestParam queueType: String)
    {
        val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
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
            messageToRelease.consumedBy = null
            messageToRelease.consumed = false
            ResponseEntity.ok(messageToRelease)
        }

        // No entries match the provided UUID (and queue type)
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid. (Queue-type: $queueType.")
    }

    @DeleteMapping
    fun removeMessage(@RequestParam uuid: String, @RequestParam queueType: String, @RequestParam(required = false) consumedBy: String?)
    {
        val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(queueType)
        val message = queueForType.stream().filter { message -> message.uuid.toString() == uuid }.findFirst()
        if (message.isPresent)
        {
            val messageToRemove = message.get()
            if ( !consumedBy.isNullOrBlank())
            {
                if (messageToRemove.consumedBy == consumedBy)
                {
                    queueForType.remove(messageToRemove)
                }
                else
                {
                    throw ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to remove message with UUID $uuid in Queue $queueType because the provided consumedBy: $consumedBy does not match the message's consumedBy: ${messageToRemove.consumedBy}")
                }
            }
            else
            {
                queueForType.remove(messageToRemove)
            }
            ResponseEntity.noContent()
        }
        else
        {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find message with UUID $uuid. (Queue-type: $queueType.")
        }
    }
}
