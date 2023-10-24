package au.kilemon.messagequeue.authentication

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 *
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

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this("")

    constructor(authenticationMatrix: AuthenticationMatrix) : this()
    {
        this.id = authenticationMatrix.id
        this.subQueue = authenticationMatrix.subQueue
    }
}
