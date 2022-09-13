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
import java.util.*
import java.util.stream.Collectors
import javax.validation.Valid
import kotlin.collections.HashMap

/**
 * @author github.com/KyleGonzalez
 */
@RestController
@RequestMapping(MessageQueueController.MESSAGE_QUEUE_BASE_PATH)
class MessageQueueController
{
    companion object
    {
        const val MESSAGE_QUEUE_BASE_PATH: String = "/message/queue"
        const val ENDPOINT_ALL: String = "/all"
        const val ENDPOINT_ENTRY: String = "/entry"
        const val ENDPOINT_OWNED: String = "/owned"

        const val ENDPOINT_CONSUME: String = "/consume"
        const val ENDPOINT_RELEASE: String = "/release"
        const val ENDPOINT_NEXT: String = "/next"
    }

    @Autowired
    lateinit var messageQueue: MultiQueue

    @GetMapping("$ENDPOINT_ENTRY/{queueType}",
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getValue(@PathVariable queueType: String?): ResponseEntity<String>
    {
        return if (queueType == null)
        {
            ResponseEntity.ok(messageQueue.size.toString())
        }
        else
        {
            ResponseEntity.ok(messageQueue.getQueueForType(queueType).size.toString())
        }
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
            throw Exception("Failed to add entry")
        }
    }

    /**
     * A [GetMapping] endpoint which retrieves all the stored [QueueMessage]s that are currently available in the [MultiQueue].
     *
     * @param detailed *true* if you require detailed information about each message and their payload/owner, otherwise **false** which displayed only limited information about each message
     * @return a [Map] where the `key` is the `queueType` and the `value` is a comma separated list of all the [QueueMessage.toDetailedString]
     */
    @GetMapping(ENDPOINT_ALL,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(@RequestParam(required = false) detailed: Boolean?): ResponseEntity<Map<String, String>>
    {
        val responseMap = HashMap<String, String>()
        for (key: String in messageQueue.keys())
        {
            val queueForType: Queue<QueueMessage> = messageQueue.getQueueForType(key)
            if (queueForType.isNotEmpty())
            {
                val queueDetails = queueForType.stream().map { message -> message.toDetailedString(detailed) }.collect(Collectors.toList())
                responseMap[key] = queueDetails.toString()
            }
        }
        return ResponseEntity.ok(responseMap)
    }

    @GetMapping(ENDPOINT_OWNED,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOwned(@RequestParam consumedBy: String, @RequestParam(required = false) queueType: String?)
    {

    }

    @PutMapping(ENDPOINT_CONSUME,
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun consumeMessage(@RequestParam(required = false) queueType: String?, @RequestParam uuid: String, @RequestParam consumedBy: String)
    {

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
    fun releaseMessage(@RequestParam(required = false) queueType: String?, @RequestParam uuid: String)
    {

    }

    @DeleteMapping
    fun removeMessage(@RequestParam uuid: String, @RequestParam queueType: String)
    {

    }
}
