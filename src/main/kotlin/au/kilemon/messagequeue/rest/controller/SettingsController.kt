package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.settings.MessageQueueSettings
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * An endpoint which contains information about the running application.
 *
 * @author github.com/Kilemonn
 */
@Tag(name = SettingsController.SETTINGS_TAG)
@RestController
@RequestMapping(SettingsController.SETTINGS_PATH)
open class SettingsController
{
    companion object
    {
        /**
         * The [Tag] for the [SettingsController] endpoints.
         */
        const val SETTINGS_TAG: String = "Settings"

        /**
         * The base path for the [SettingsController].
         */
        const val SETTINGS_PATH = "/settings"
    }

    @Autowired
    private lateinit var queueSettings: MessageQueueSettings

    /**
     * Get and return the [MessageQueueSettings] singleton for the user to view configuration.
     */
    @Operation(summary = "Retrieve queue settings configuration.", description = "Retrieve information about the queue settings and run time configuration.")
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiResponse(responseCode = "200", description = "Successfully returns the queue settings.")
    fun getSettings(): ResponseEntity<MessageQueueSettings>
    {
        return ResponseEntity.ok(queueSettings)
    }
}
