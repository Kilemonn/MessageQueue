package au.kilemon.messagequeue.rest.controller.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * A model used during the message create request so we are able to only expose and allow the user to set fields we allow.
 * Specifically we do not want the user to be able to provide their own UUID anymore, since it must be a v7 UUID that we generate.
 *
 * @author github.com/Kilemonn
 */
class CreateMessage
{
    @Schema(title = "Sub-queue identifier", example = "my-queue-name",
        description = "The sub-queue identifier for the sub-queue that this message is stored in.")
    @NotBlank
    lateinit var subQueue: String

    @Schema(title = "Assignee identifier", example = "owner-id",
        description = "The unique identifier of assignee who currently possessions or owns this message.")
    var assignedTo: String? = null

    @Schema(description = "The message payload, this can be any type of complex or simple object that you wish.")
    var payload: Any? = null
}
