package au.kilemon.messagequeue.queue.sql

import java.util.*

/**
 * An enum holding the SQL driver and dialect for the supported database servers.
 * This is used to determine that the provided arguments are appropriate and supported by the application.
 *
 * @author github.com/KyleGonzalez
 */
enum class SqlType(val driverName: String, val dialects: List<String>)
{
    MYSQL("com.mysql.jdbc.Driver", listOf("org.hibernate.dialect.MySQLDialect", "org.hibernate.dialect.MySQLInnoDBDialect", "org.hibernate.dialect.MySQLMyISAMDialect")),
    POSTGRES("org.postgresql.Driver", listOf("org.hibernate.dialect.PostgreSQLDialect")),
    ORACLE("oracle.jdbc.driver.OracleDriver", listOf("org.hibernate.dialect.OracleDialect", "org.hibernate.dialect.Oracle9iDialect", "org.hibernate.dialect.Oracle10gDialect")),
    MICROSOFT("com.microsoft.sqlserver.jdbc.SQLServerDriver", listOf("org.hibernate.dialect.SQLServerDialect"));

    companion object
    {
        /**
         * Get the corresponding [SqlType] based on the provided [driverName].
         *
         * @param driverName the name of the driver to match
         * @return the matching [SqlType] if the provided [driverName] matches the [SqlType.driverName]
         */
        private fun fromDriverName(driverName: String): SqlType?
        {
            val element = Arrays.stream(values()).filter{ type -> type.driverName == driverName}.findFirst()
            return if (element.isPresent)
            {
                element.get()
            }
            else
            {
                null
            }
        }

        /**
         * Get the corresponding [SqlType] based on the provided [dialect].
         * The provided [dialect] must be in the [SqlType.dialects] list.
         *
         * @param dialect the name of the dialect to match
         * @return the matching [SqlType] if the provided [dialect] matches any of the [SqlType.dialects]
         */
        private fun fromDialectName(dialect: String): SqlType?
        {
            val element = Arrays.stream(values()).filter{ type -> type.dialects.contains(dialect)}.findFirst()
            return if (element.isPresent)
            {
                element.get()
            }
            else
            {
                null
            }
        }

        /**
         * Determine whether the provided [driverName] and [dialectName] match the same [SqlType].
         * This calls [SqlType.fromDriverName] and [SqlType.fromDialectName] to determine whether the [SqlType]s are the same.
         *
         * @param driverName the name of the driver to look up
         * @param dialectName the name of the dialect to look up
         * @return `true` if the [SqlType]s resolved are the same, otherwise `false`
         */
        fun matchingDriverAndDialect(driverName: String, dialectName: String): Boolean
        {
            val driver = fromDriverName(driverName)
            val dialect = fromDialectName(dialectName)
            return driver == dialect
        }
    }
}
