package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.Payload
import au.kilemon.messagequeue.PayloadEnum
import au.kilemon.messagequeue.exception.DuplicateMessageException
import au.kilemon.messagequeue.message.QueueMessage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
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
abstract class AbstractMultiQueueTest<T: MultiQueue>
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
        val message = QueueMessage(data, "type")
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(1, multiQueue.size)

        val retrievedMessage = multiQueue.pollForType(message.type)
        Assertions.assertTrue(multiQueue.isEmpty())
        Assertions.assertEquals(0, multiQueue.size)

        Assertions.assertTrue(retrievedMessage.isPresent)
        Assertions.assertEquals(data, retrievedMessage.get().payload)
    }

    /**
     * An argument provider for the [AbstractMultiQueueTest.testAdd] method.
     */
    private fun parameters_testAdd(): Stream<Arguments>
    {
        return Stream.of(
            Arguments.of(1234),
            Arguments.of("a string"),
            Arguments.of(listOf("element1", "element2", "element3")),
            Arguments.of(true)
        )
    }

    /***
     * Test [MultiQueue.add] to ensure that [DuplicateMessageException] is thrown if a [QueueMessage] already exists with the same `UUID` even if it is assigned to a different `queue type`.
     */
    @Test
    fun testAdd_entryAlreadyExistsInDifferentQueueType()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage("test", "type")
        Assertions.assertTrue(multiQueue.add(message))

        val differentType = "different-type"
        val differentTypeMessage = QueueMessage(message.payload, differentType)
        differentTypeMessage.uuid = message.uuid

        Assertions.assertEquals(message.payload, differentTypeMessage.payload)
        Assertions.assertEquals(message.uuid, differentTypeMessage.uuid)
        Assertions.assertNotEquals(message.type, differentTypeMessage.type)

        assertThrows(DuplicateMessageException::class.java)
        {
            multiQueue.add(differentTypeMessage)
        }
    }

    /***
     * Test [MultiQueue.add] to ensure that [DuplicateMessageException] is thrown if a [QueueMessage] already exists with the same `UUID` even if it is assigned to the same `queue type`.
     */
    @Test
    fun testAdd_sameEntryAlreadyExists()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage("test", "type")
        Assertions.assertTrue(multiQueue.add(message))

        assertThrows(DuplicateMessageException::class.java)
        {
            multiQueue.add(message)
        }
    }

    /**
     * Ensure that when an entry is added and the same entry is removed that the [MultiQueue] is empty.
     */
    @Test
    fun testRemove()
    {
        Assertions.assertTrue(multiQueue.isEmpty())

        val message = QueueMessage("A test value", "type")

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
        val messageThatDoesntExist = QueueMessage(Payload("some Other data", 23, false, PayloadEnum.A), "type")

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
        val type = "type"
        val otherData = Payload("some Other data", 65, true, PayloadEnum.B)
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
        val message = QueueMessage(0x52347, "type")

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
        val message = QueueMessage(0x5234, "type")

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())

        Assertions.assertTrue(multiQueue.contains(message))
        message.consumed = true
        message.consumedBy = "Instance_11242"
        Assertions.assertTrue(multiQueue.contains(message))
    }

    /**
     * Ensure that all elements are added, and contained and removed via the provided [Collection].
     */
    @Test
    fun testAddAll_containsAll_removeAll()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val list = listOf(QueueMessage(81273648, "type"), QueueMessage("test test test", "type"))
        Assertions.assertTrue(multiQueue.addAll(list))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(2, multiQueue.size)

        Assertions.assertTrue(multiQueue.containsAll(list))
        Assertions.assertTrue(multiQueue.contains(list[0]))
        Assertions.assertTrue(multiQueue.contains(list[1]))

        Assertions.assertTrue(multiQueue.removeAll(list))
        Assertions.assertTrue(multiQueue.isEmpty())
        Assertions.assertEquals(0, multiQueue.size)
    }

    /**
     * Ensure that `null` is returned when there are no elements in the [MultiQueue] for the specific queue.
     * Otherwise, if it does exist make sure that the correct entry is returned and that it is removed.
     */
    @Test
    fun testPollForType()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage(Payload("poll for type", 89, true, PayloadEnum.B), "poll-type")

        Assertions.assertFalse(multiQueue.pollForType(message.type).isPresent)
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(message, multiQueue.pollForType(message.type).get())
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    /**
     * Ensure that `null` is returned when there are no elements in the [MultiQueue] for the specific queue.
     * Otherwise, if it does exist make sure that the correct entry is returned.
     */
    @Test
    fun testPeekForType()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage(Payload("peek for type", 1121, false, PayloadEnum.C), "peek-type")

        Assertions.assertFalse(multiQueue.peekForType(message.type).isPresent)
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(message, multiQueue.peekForType(message.type).get())
        Assertions.assertFalse(multiQueue.isEmpty())

    }

    /**
     * Ensure that [MultiQueue.isEmptyForType] operates as expected when entries exist and don't exist for a specific type.
     */
    @Test
    fun testIsEmptyForType()
    {
        Assertions.assertTrue(multiQueue.isEmpty())

        val type = "type"
        val data = "test data"
        val message = QueueMessage(data, type)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertFalse(multiQueue.isEmptyForType(type))
        Assertions.assertTrue(multiQueue.isEmptyForType("another-type"))
    }

    /**
     * Ensure that only the specific entries are removed when [MultiQueue.clearForType] is called.
     */
    @Test
    fun testClearForType()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = "clear-for-type"
        val list = listOf(QueueMessage(81273648, type), QueueMessage("test test test", type))
        Assertions.assertTrue(multiQueue.addAll(list))

        val singleEntryType = "single-entry-type"
        val message = QueueMessage("test message", singleEntryType)
        Assertions.assertTrue(multiQueue.add(message))

        Assertions.assertEquals(3, multiQueue.size)
        multiQueue.clearForType(type)
        Assertions.assertEquals(1, multiQueue.size)
        multiQueue.clearForType(singleEntryType)
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    /**
     * Ensure that no change is made when the specific type has no entries.
     */
    @Test
    fun testClearForType_DoesNotExist()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = "clear-for-type-does-not-exist"
        multiQueue.clearForType(type)
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    /**
     * Ensure that the correct entries are retained and that the correct `Boolean` value is returned.
     */
    @Test
    fun testRetainAll()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val type = "type1"
        val type2 = "type2"
        val data = Payload("some payload", 1, true, PayloadEnum.A)
        val data2 = Payload("some more data", 2, false, PayloadEnum.B)
        val list = listOf(QueueMessage(data, type), QueueMessage(data, type2), QueueMessage(data2, type), QueueMessage(data2, type2))

        Assertions.assertTrue(multiQueue.addAll(list))
        Assertions.assertEquals(4, multiQueue.size)

        val toRetain = ArrayList<QueueMessage>()
        toRetain.addAll(list.subList(0, 2))
        Assertions.assertEquals(2, toRetain.size)
        // No elements of this type to cover all branches of code
        val type3 = "type3"
        val type3Message = QueueMessage(Payload("type3 data", 3, false, PayloadEnum.C), type3)
        toRetain.add(type3Message)
        Assertions.assertEquals(3, toRetain.size)

        Assertions.assertTrue(multiQueue.retainAll(toRetain))
        Assertions.assertEquals(2, multiQueue.size)
        Assertions.assertTrue(multiQueue.contains(list[0]))
        Assertions.assertTrue(multiQueue.contains(list[1]))

        Assertions.assertFalse(multiQueue.contains(list[2]))
        Assertions.assertFalse(multiQueue.contains(list[3]))
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
                    multiQueue.offer(QueueMessage(Payload("test data", 13, false, PayloadEnum.C), "test type"))
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
