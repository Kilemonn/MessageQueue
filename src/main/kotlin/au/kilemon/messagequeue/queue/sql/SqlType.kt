package au.kilemon.messagequeue.queue.sql

/**
 *
 * @author github.com/KyleGonzalez
 */
enum class SqlType(private val driverName: String, private val dialect: List<String>)
{
    MY_SQL("com.mysql.jdbc.Driver", listOf("org.hibernate.dialect.MySQLDialect", "org.hibernate.dialect.MySQLInnoDBDialect", "org.hibernate.dialect.MySQLMyISAMDialect")),
    POSTGRES("org.postgresql.Driver", listOf("org.hibernate.dialect.PostgreSQLDialect")),
    ORACLE("oracle.jdbc.driver.OracleDriver", listOf("org.hibernate.dialect.OracleDialect", "org.hibernate.dialect.Oracle9iDialect", "org.hibernate.dialect.Oracle10gDialect")),
    MICROSOFT("com.microsoft.sqlserver.jdbc.SQLServerDriver", listOf("org.hibernate.dialect.SQLServerDialect"));

    companion object
    {
        private fun fromDriverName(driverName: String): SqlType?
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

        private fun fromDialectName(dialect: String): SqlType?
        {
            for (type in values())
            {
                if (type.dialect.contains(dialect))
                {
                    return type
                }
            }
            return null
        }

        fun matchingDriverAndDialect(driverName: String, dialectName: String): Boolean
        {
            val driver = fromDriverName(driverName)
            val dialect = fromDialectName(dialectName)
            return driver == dialect
        }
    }
}
