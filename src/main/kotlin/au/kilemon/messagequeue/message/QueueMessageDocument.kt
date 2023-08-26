package au.kilemon.messagequeue.message

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.util.SerializationUtils
import java.util.*
import javax.persistence.*

/**
 * This is used for `No-SQL` queues.
 *
 * @author github.com/Kilemonn
 */
@Document
class QueueMessageDocument(var payload: Any?, @Column(nullable = false) var type: String, @Column(name = "assignedto") var assignedTo: String? = null)
{
    @Column(nullable = false, unique = true)
    var uuid: String = UUID.randomUUID().toString()

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    /**
     * Required for JSON deserialisation.
     */
    constructor() : this(null, "")
}
