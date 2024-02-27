package au.kilemon.messagequeue.authentication

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable

/**
 * An object that holds subqueue authentication information.
 * If a specific sub-queue is in restricted mode it will have a matching [AuthenticationMatrix] created which will
 * be checked to verify if a specific sub-queue can be operated on.
 * This object is used for `In-memory`, `SQL` and `Redis`.
 *
 * @author github.com/Kilemonn
 */
@Entity
@Table(name = AuthenticationMatrix.TABLE_NAME)
class AuthenticationMatrix(@Column(name = "subqueue", nullable = false) var subQueue: String): Serializable
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
     * Required for MySQL.
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
