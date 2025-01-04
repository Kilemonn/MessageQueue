package au.kilemon.messagequeue.rest.controller

import lombok.Generated
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import kotlin.math.max
import kotlin.math.min

/**
 * A collection of constants used as REST parameters.
 *
 * @author github.com/Kilemonn
 */
@Generated
object RestParameters
{
    const val ASSIGNED_TO = "assignedTo"

    const val SUB_QUEUE = "subQueue"

    const val DETAILED = "detailed"

    const val UUID = "uuid"

    const val INCLUDE_EMPTY = "includeEmpty"

    const val EXPIRY = "expiry"

    const val CLEAR_QUEUE = "clearQueue"

    const val PAGE = "page"
    const val DEFAULT_PAGE_NUMBER = 1
    const val PAGE_SIZE = "pageSize"

    /**
     * Default and max page size
     */
    const val DEFAULT_PAGE_SIZE = 100

    /**
     * Making sure the page number is not negative or zero.
     */
    fun getPageNumber(pageNum: Int): Int
    {
        return max(DEFAULT_PAGE_NUMBER, pageNum)
    }

    /**
     * Greater than zero too.
     */
    fun getPageSize(pageSize: Int): Int
    {
        return min(max(1, pageSize), DEFAULT_PAGE_SIZE)
    }
}
