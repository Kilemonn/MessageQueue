package au.kilemon.messagequeue

import lombok.Generated
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * The main application class.
 *
 * @author github.com/KyleGonzalez
 */
@SpringBootApplication
open class MessageQueueApplication
{
    companion object
    {
        /**
         * Application version number, make sure this matches what is defined in `build.gradle.kts`.
         */
        const val VERSION: String = "0.1.9"
    }
}

/**
 * The application entry point.
 *
 * @param args program commandline arguments
 */
@Generated // Skip coverage on this method
fun main(args: Array<String>)
{
    runApplication<MessageQueueApplication>(*args)
}
