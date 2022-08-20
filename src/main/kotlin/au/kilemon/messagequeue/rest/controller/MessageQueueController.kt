package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.response.MessageResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/message/queue")
class MessageQueueController
{
    @Autowired
    lateinit var messageQueue: MultiQueue

    @GetMapping
    fun getValue(): String
    {
        return messageQueue.size.toString()
    }

    @PostMapping(
        path = ["/{queueType}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE])
    fun createMessage(@PathVariable queueType: String, @Valid @RequestBody queueMessage: QueueMessage): MessageResponse
    {
        return MessageResponse(queueType=queueType)
    }
}
