package au.kilemon.messagequeue.settings

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Test the [MessageQueueSettings] values when they are set via the system properties.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = [
    "${MessageQueueSettings.STORAGE_MEDIUM}=REDIS",
    "${MessageQueueSettings.REDIS_ENDPOINT}=123.123.123.123",
    "${MessageQueueSettings.REDIS_PREFIX}=redis",
    "${MessageQueueSettings.REDIS_USE_SENTINELS}=true",
    "${MessageQueueSettings.REDIS_MASTER_NAME}=master"
])
class MessageQueueSettingsTest
{
    /**
     * A [TestConfiguration] for the [MessageQueueSettingsDefaultTest] class.
     *
     * @author github.com/Kilemonn
     */
    @TestConfiguration
    internal class MessageQueueSettingsTestConfiguration
    {
        @Bean
        open fun getMessageQueueSettings(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }
    }

    @Autowired
    lateinit var messageQueueSettings: MessageQueueSettings

    /**
     * Test the default values of the [MessageQueueSettings] ensuring that they are initialised correctly
     * when no properties are explicitly set.
     */
    @Test
    fun testValues()
    {
        Assertions.assertNotNull(messageQueueSettings)
        Assertions.assertEquals("REDIS", messageQueueSettings.storageMedium)
        Assertions.assertEquals("123.123.123.123", messageQueueSettings.redisEndpoint)
        Assertions.assertEquals("redis", messageQueueSettings.redisPrefix)
        Assertions.assertEquals("master", messageQueueSettings.redisMasterName)
        Assertions.assertEquals(true.toString(), messageQueueSettings.redisUseSentinels)
    }
}
