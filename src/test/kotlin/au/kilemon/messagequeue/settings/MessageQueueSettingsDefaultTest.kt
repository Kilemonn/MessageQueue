package au.kilemon.messagequeue.settings

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Test the [MessageQueueSettings] default annotation values are set correctly.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
class MessageQueueSettingsDefaultTest
{
    /**
     * A [TestConfiguration] for the [MessageQueueSettingsDefaultTest] class.
     *
     * @author github.com/Kilemonn
     */
    @TestConfiguration
    internal class MessageQueueSettingsDefaultTestConfiguration
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
    fun testDefaults()
    {
        Assertions.assertNotNull(messageQueueSettings)
        Assertions.assertEquals(MessageQueueSettings.MULTI_QUEUE_TYPE_DEFAULT, messageQueueSettings.multiQueueType)
        Assertions.assertEquals(MessageQueueSettings.MULTI_QUEUE_AUTHENTICATION_DEFAULT, messageQueueSettings.multiQueueAuthentication)

        Assertions.assertEquals(MessageQueueSettings.REDIS_ENDPOINT_DEFAULT, messageQueueSettings.redisEndpoint)
        Assertions.assertEquals("", messageQueueSettings.redisPrefix)
        Assertions.assertEquals(MessageQueueSettings.REDIS_MASTER_NAME_DEFAULT, messageQueueSettings.redisMasterName)
        Assertions.assertEquals(false.toString(), messageQueueSettings.redisUseSentinels)

        Assertions.assertEquals("", messageQueueSettings.sqlEndpoint)
        Assertions.assertEquals("", messageQueueSettings.sqlUsername)

        Assertions.assertEquals("", messageQueueSettings.mongoHost)
        Assertions.assertEquals("", messageQueueSettings.mongoUri)
        Assertions.assertEquals("", messageQueueSettings.mongoPort)
        Assertions.assertEquals("", messageQueueSettings.mongoDatabase)
        Assertions.assertEquals("", messageQueueSettings.mongoUsername)
    }
}
