package au.kilemon.messagequeue.authentication

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * A holder object for the restricted sub-queues. Refer to [AuthenticationMatrix].
 * This is used only for `Mongo`.
 *
 * @author github.com/Kilemonn
 */
@Document(value = AuthenticationMatrixDocument.DOCUMENT_NAME)
class AuthenticationMatrixDocument(var subQueue: String)
{
    companion object
    {
        const val DOCUMENT_NAME: String = "multiqueueauthenticationmatrix"
    }

    @JsonIgnore
    @Id
    var id: Long? = null
}
