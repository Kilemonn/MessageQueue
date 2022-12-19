package au.kilemon.messagequeue.queue.sql

/**
 * An enum holding the SQL driver and dialect for the supported database servers.
 * This is just an internal class that is used to hold some existing constants for drivers that hibernate supports.
 *
 * @author github.com/KyleGonzalez
 */
enum class SqlType(val driverName: String, val dialects: List<String>)
{
    MYSQL("com.mysql.jdbc.Driver", listOf("org.hibernate.dialect.MySQLDialect", "org.hibernate.dialect.MySQLInnoDBDialect", "org.hibernate.dialect.MySQLMyISAMDialect")),
    POSTGRES("org.postgresql.Driver", listOf("org.hibernate.dialect.PostgreSQLDialect")),
    ORACLE("oracle.jdbc.driver.OracleDriver", listOf("org.hibernate.dialect.OracleDialect", "org.hibernate.dialect.Oracle9iDialect", "org.hibernate.dialect.Oracle10gDialect")),
    MICROSOFT("com.microsoft.sqlserver.jdbc.SQLServerDriver", listOf("org.hibernate.dialect.SQLServerDialect"));
}
