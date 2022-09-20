package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * An endpoint which contains information about the running application.
 *
 * @author github.com/KyleGonzalez
 */
@RestController
@RequestMapping(SettingsController.SETTINGS_PATH)
open class SettingsController
{
    companion object
    {
        /**
         * The base path for the [SettingsController].
         */
        const val SETTINGS_PATH = "/message/settings"
    }

    @Autowired
    lateinit var queueSettings: MessageQueueSettings

    /**
     * Get and return the [MessageQueueSettings] singleton for the user to view configuration.
     */
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getSettings(): ResponseEntity<MessageQueueSettings>
    {
        return ResponseEntity.ok(queueSettings)
    }
}
