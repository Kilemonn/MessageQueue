package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 * @author github.com/KyleGonzalez
 */
@RestController
@RequestMapping(SettingsController.SETTINGS_PATH)
class SettingsController
{
    companion object
    {
        const val SETTINGS_PATH = "/message/settings"
    }

    @Autowired
    lateinit var queueSettings: MessageQueueSettings

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getSettings(): ResponseEntity<MessageQueueSettings>
    {
        return ResponseEntity.ok(queueSettings)
    }
}
