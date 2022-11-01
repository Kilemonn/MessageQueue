package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.settings.MessageQueueSettings.Companion.SQL_TABLE_NAME_DEFAULT

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

    fun getCreateStatement(): String
    {
        when (this)
        {
            MY_SQL ->
            {
                return mysqlCreateStatement()
            }
            POSTGRES ->
            {
                return postgresCreateStatement()
            }
            ORACLE ->
            {
                return oracleCreateStatement()
            }
            else -> return ""
        }
    }

    private fun postgresCreateStatement(): String
    {
        return "CREATE TABLE $SQL_TABLE_NAME_DEFAULT (\n" +
                "   id SERIAL PRIMARY KEY,\n" +
                "   type VARCHAR(200) NOT NULL,\n" +
                "   \"uuid\" UUID NOT NULL,\n" +
                "   assigned BOOLEAN DEFAULT FALSE,\n" +
                "   assignedto VARCHAR(200),\n" +
                "   payload JSON NOT NULL,\n" +
                "   CONSTRAINT unique_uuid UNIQUE (\"uuid\")\n" +
                ")"
    }

    private fun mysqlCreateStatement(): String
    {
        return "CREATE TABLE $SQL_TABLE_NAME_DEFAULT (\n" +
                "    PersonID int,\n" +
                "    LastName varchar(255),\n" +
                "    FirstName varchar(255),\n" +
                "    Address varchar(255),\n" +
                "    City varchar(255)\n" +
                ");"
    }

    private fun oracleCreateStatement(): String
    {
        return ""
    }
}
