package au.kilemon.messagequeue.queue.sql

/**
 *
 * @author github.com/KyleGonzalez
 */
enum class SqlType(private val driverName: String)
{
    MY_SQL("com.mysql.jdbc.Driver"),
    POSTGRES("org.postgresql.Driver"),
    ORACLE("oracle.jdbc.driver.OracleDriver");

    companion object
    {
        fun fromDriverName(driverName: String): SqlType?
        {
            for (type in values())
            {
                if (type.driverName == driverName)
                {
                    return type
                }
            }
            return null
        }
    }
}
