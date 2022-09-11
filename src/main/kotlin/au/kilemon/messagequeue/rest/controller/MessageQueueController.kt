package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.MessageResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

/**
 * @author github.com/KyleGonzalez
 */
@RestController
@RequestMapping(MessageQueueController.MESSAGE_QUEUE_PATH)
class MessageQueueController
{
    companion object
    {
        const val MESSAGE_QUEUE_PATH: String = "/message/queue"
    }

    @Autowired
    lateinit var messageQueue: MultiQueue

    @GetMapping(path = ["/{queueType}"],
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

    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
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
}
