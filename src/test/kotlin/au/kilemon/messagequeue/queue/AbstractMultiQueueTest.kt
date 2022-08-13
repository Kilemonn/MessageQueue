package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.Payload
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.type.QueueType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.Serializable
import java.util.stream.Stream

/**
 * An abstract test class for the [MultiQueue] class.
 * This class can be extended, and the [MultiQueue] member overridden to easily ensure that the different
 * [MultiQueue] implementations all operate as expected in the same test cases.
 *
 * @author github.com/KyleGonzalez
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
abstract class AbstractMultiQueueTest<T: MultiQueue<QueueMessage>>
{
    @Autowired
    protected lateinit var multiQueue: T

    /**
     * Ensure the [MultiQueue] is cleared before each test.
     */
    @BeforeEach
    fun setup()
    {
        multiQueue.clear()
        duringSetup()
    }

    /**
     * Called in the [BeforeEach] after the parent has done its preparation.
     */
    abstract fun duringSetup()

    /**
     * Ensure that when a new entry is added, that the [MultiQueue] is no longer empty and reports the correct size.
     *
     * @param data the incoming [Serializable] data to store in the [MultiQueue] to test that we can cater for multiple types
     */
    @ParameterizedTest
    @MethodSource("parameters_testAdd")
    fun testAdd(data: Serializable)
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = QueueType("type")
        val message = QueueMessage(data, type)
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(1, multiQueue.size)

        val retrievedMessage = multiQueue.pollForType(type)
        Assertions.assertTrue(multiQueue.isEmpty())
        Assertions.assertEquals(0, multiQueue.size)

        Assertions.assertNotNull(retrievedMessage)
        Assertions.assertEquals(data, retrievedMessage!!.data)
    }

    /**
     * An argument provider for the [AbstractMultiQueueTest.testAdd] method.
     */
    private fun parameters_testAdd(): Stream<Arguments>
    {
        return Stream.of(
            Arguments.of(1234),
            Arguments.of("a string"),
            Arguments.of(listOf("element1", "element2", "element3"))
        )
    }

    /**
     * Ensure that when an entry is added and the same entry is removed that the [MultiQueue] is empty.
     */
    @Test
    fun testRemove()
    {
        Assertions.assertTrue(multiQueue.isEmpty())

        val type = QueueType("type")
        val data = "A test value"
        val message = QueueMessage(data, type)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(1, multiQueue.size)

        Assertions.assertTrue(multiQueue.remove(message))
        Assertions.assertTrue(multiQueue.isEmpty())
        Assertions.assertEquals(0, multiQueue.size)
    }

    /**
     * Ensure that if an entry that does not exist is attempting to be removed, then `false` is returned from the [MultiQueue.remove] method.
     */
    @Test
    fun testRemove_whenEntryDoesntExist()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = QueueType("type")
        val otherData = Payload("some Other data")
        val messageThatDoesntExist = QueueMessage(otherData, type)

        Assertions.assertTrue(multiQueue.isEmptyForType(type))
        Assertions.assertFalse(multiQueue.remove(messageThatDoesntExist))
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    /**
     * Ensure that `false` is returned from [MultiQueue.contains] when the entry does not exist.
     */
    @Test
    fun testContains_whenEntryDoesntExist()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = QueueType("type")
        val otherData = Payload("some Other data")
        val messageThatDoesntExist = QueueMessage(otherData, type)
        Assertions.assertFalse(multiQueue.contains(messageThatDoesntExist))
    }

    /**
     * Ensure that `true` is returned from [MultiQueue.contains] when the entry does exist.
     */
    @Test
    fun testContains_whenEntryExists()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = QueueType("type")
        val data = 0x5234
        val message = QueueMessage(data, type)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(1, multiQueue.size)
        Assertions.assertTrue(multiQueue.contains(message))
    }

    /**
     * Ensure that `true` is returned from [MultiQueue.contains] when the entry does exist.
     * And when the `@EqualsAndHashCode.Exclude` properties are changed. This is the make sure that even if
     * we change some metadata properties, that we can still find the correct entry, since the metadata fields
     * should be ignored when the [QueueMessage.equals] method is called.
     */
    @Test
    fun testContains_whenMetadataPropertiesAreSet()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = QueueType("type")
        val data = 0x5234
        val message = QueueMessage(data, type)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        message.isConsumed = true
        message.consumedBy = "Instance_11242"
        Assertions.assertTrue(multiQueue.contains(message))
    }

    /**
     * Ensure that all applicable methods throw an [UnsupportedOperationException].
     */
    @Test
    fun testUnsupportedMethods()
    {
        assertAll( "Unsupported methods",
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.peek()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.offer(QueueMessage(Payload("test data"), QueueType("test type")))
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.element()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.poll()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.remove()
                }
            },
            {
                assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.iterator()
                }
            }
        )
    }
}
