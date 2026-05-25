package au.kilemon.messagequeue.authentication.token

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * A base test class for [JwtTokenProvider] and different token issuing and verification scenarios.
 *
 * @author github.com/Kilemonn
 */
@SpringBootTest(classes = [JwtTokenProvider::class, QueueConfiguration::class,
    MessageQueueSettings::class, LoggingConfiguration::class
])
abstract class JwtTokenProviderTest
{
    @Autowired
    protected lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    protected lateinit var restrictionMode: RestrictionMode
}
