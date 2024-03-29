package au.kilemon.messagequeue.queue.sql

import au.kilemon.messagequeue.queue.MultiQueueTest
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * A base class for DB tests. Containing class level annotations and the [TestConfiguration] class required by all DB tests.
 *
 * Properties to assist with debugging:
 * - "hibernate.show_sql=true"
 * - "hibernate.format_sql=true",
 * - "logging.level.org.hibernate.type=TRACE",
 * - "spring.jpa.show-sql=true",
 * - "spring.jpa.properties.hibernate.format_sql=true"
 *
 * Need to define `spring.autoconfigure.exclude=` as empty to override the default in `application.properties` otherwise JPA will no auto initialise.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@Testcontainers
@DataJpaTest(properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=SQL", "spring.jpa.hibernate.ddl-auto=create", "spring.autoconfigure.exclude="])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class SqlMultiQueueTest: MultiQueueTest()
