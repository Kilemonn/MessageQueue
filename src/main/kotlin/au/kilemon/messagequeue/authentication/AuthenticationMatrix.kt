package au.kilemon.messagequeue.authentication

import au.kilemon.messagequeue.message.QueueMessage
import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

/**
 *
 *
 * @author github.com/Kilemonn
 */
@Entity
@Table(name = AuthenticationMatrix.TABLE_NAME)
class AuthenticationMatrix(@Column(name = "subqueue", nullable = false) var subQueue: String)
{
    companion object
    {
        const val TABLE_NAME: String = "multiqueueauthenticationmatrix"
    }

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this("")

    /**
     * Overriding to only include specific properties when checking if messages are equal.
     * This checks the following are equal in order to return `true`:
     * - subQueue
     */
    override fun equals(other: Any?): Boolean
    {
        if (other == null || other !is AuthenticationMatrix)
        {
            return false
        }

        return other.subQueue == this.subQueue
    }

    /**
     * Only performs a hashcode on the properties checked in [AuthenticationMatrix.equals].
     */
    override fun hashCode(): Int
    {
        return subQueue.hashCode()
    }
}
