package au.kilemon.messagequeue.controller

import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("message/settings")
class SettingsController
{
    @Autowired
    lateinit var queueSettings: MessageQueueSettings

    @GetMapping
    @ResponseBody
    fun getSettings(): MessageQueueSettings
    {
        return queueSettings
    }
}
