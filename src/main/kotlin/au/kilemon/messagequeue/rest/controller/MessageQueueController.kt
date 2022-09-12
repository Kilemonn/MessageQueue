package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.MessageResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.validation.Valid

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
    fun getValue(@PathVariable queueType: String?): String
    {
        return if (queueType == null)
        {
            messageQueue.size.toString()
        }
        else
        {
            messageQueue.getQueueForType(queueType).size.toString()
        }
    }

    @PostMapping(ENDPOINT_ENTRY,
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createMessage(@Valid @RequestBody queueMessage: QueueMessage): MessageResponse
    {
        val wasAdded = messageQueue.add(queueMessage)
        if (wasAdded)
        {
            return MessageResponse(message=queueMessage)
        }
        else
        {
            throw Exception("Failed to add entry")
        }
    }

    @GetMapping(ENDPOINT_ALL,
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(@RequestParam(required = false) detailed: Boolean?): Map<String, String>
    {
        val responseMap = HashMap<String, String>()
        val keys: Set<String> = messageQueue.keys()
        for (key in keys)
        {
            val queueForType = messageQueue.getQueueForType(key)
            if (queueForType.isNotEmpty())
            {
                val queueDetails = queueForType.stream().map { message -> message.toDetailedString(detailed) }.collect(Collectors.toList())
                responseMap[key] = queueDetails.toString()
            }
        }
        return responseMap
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
    fun getNext(@RequestParam queueType: String, @RequestParam consumedBy: String)
    {

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
