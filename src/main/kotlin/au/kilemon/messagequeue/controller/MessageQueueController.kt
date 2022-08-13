package au.kilemon.messagequeue.controller

import au.kilemon.messagequeue.queue.MultiQueue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
}
