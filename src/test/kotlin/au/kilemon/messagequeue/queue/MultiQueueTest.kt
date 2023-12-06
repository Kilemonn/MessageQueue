package au.kilemon.messagequeue.queue

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.exception.DuplicateMessageException
import au.kilemon.messagequeue.queue.exception.IllegalSubQueueIdentifierException
import au.kilemon.messagequeue.queue.exception.MessageUpdateException
import au.kilemon.messagequeue.queue.inmemory.InMemoryMultiQueue
import au.kilemon.messagequeue.queue.nosql.mongo.MongoMultiQueue
import au.kilemon.messagequeue.queue.sql.SqlMultiQueue
import au.kilemon.messagequeue.rest.model.Payload
import au.kilemon.messagequeue.rest.model.PayloadEnum
import au.kilemon.messagequeue.settings.MessageQueueSettings
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import java.io.Serializable
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * An abstract test class for the [MultiQueue] class.
 * This class can be extended, and the [MultiQueue] member overridden to easily ensure that the different
 * [MultiQueue] implementations all operate as expected in the same test cases.
 *
 * @author github.com/Kilemonn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class MultiQueueTest
{
    /**
     * A Spring configuration that is used for this test class.
     *
     * @author github.com/Kilemonn
     */
    @TestConfiguration
    class MultiQueueTestConfiguration
    {
        /**
         * The bean initialise here will have all its properties overridden by environment variables.
         * Don't set them here, set them in the [WebMvcTest.properties].
         */
        @Bean
        @Lazy
        open fun getMessageQueueSettingsBean(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }
    }

    @Autowired
    protected lateinit var multiQueue: MultiQueue

    @Autowired
    private lateinit var authenticator: MultiQueueAuthenticator

    /**
     * Ensure that when a new entry is added, that the [MultiQueue] is no longer empty and reports the correct size.
     *
     * @param data the incoming [Serializable] data to store in the [MultiQueue] to test that we can cater for multiple [QueueMessage.subQueue]
     */
    @ParameterizedTest
    @MethodSource("parameters_testAdd")
    fun testAdd(data: Serializable)
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage(data, "testAdd")
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertEquals(1, multiQueue.size)

        // Getting the element two ways, via the queue for type and via poll to ensure both ways resolve the object payload properly
        val queue = multiQueue.getSubQueue(message.subQueue)
        Assertions.assertEquals(1, queue.size)
        val storedElement = queue.elementAt(0)

        val retrievedMessage = multiQueue.pollSubQueue(message.subQueue)
        Assertions.assertTrue(multiQueue.isEmpty())
        Assertions.assertEquals(0, multiQueue.size)

        Assertions.assertEquals(message, storedElement)
        Assertions.assertTrue(retrievedMessage.isPresent)
        Assertions.assertEquals(message, retrievedMessage.get())

        // Triple checking the payloads are equal to ensure the resolvePayloadObject() method is called
        Assertions.assertEquals(message.payload, storedElement.payload)
        Assertions.assertEquals(message.payload, retrievedMessage.get().payload)
    }

    /**
     * An argument provider for the [MultiQueueTest.testAdd] method.
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
     * Test [MultiQueue.add] to ensure that [DuplicateMessageException] is thrown if a [QueueMessage] already exists with the same `UUID` even if it is assigned to a different `sub-queue`.
     */
    @Test
    fun testAdd_entryAlreadyExistsInDifferentSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage("test", "testAdd_entryAlreadyExistsInDifferentSubQueue")
        Assertions.assertTrue(multiQueue.add(message))

        val differentSubQueue = "testAdd_entryAlreadyExistsInDifferentSubQueue2"
        val differentMessage = QueueMessage(message.payload, differentSubQueue)
        differentMessage.uuid = message.uuid

        Assertions.assertEquals(message.payload, differentMessage.payload)
        Assertions.assertEquals(message.uuid, differentMessage.uuid)
        Assertions.assertNotEquals(message.subQueue, differentMessage.subQueue)

        Assertions.assertThrows(DuplicateMessageException::class.java)
        {
            multiQueue.add(differentMessage)
        }
    }

    /***
     * Test [MultiQueue.add] to ensure that [DuplicateMessageException] is thrown if a [QueueMessage] already exists with the same `UUID` even if it is assigned to the same `sub-queue`.
     */
    @Test
    fun testAdd_sameEntryAlreadyExists()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage("test", "testAdd_sameEntryAlreadyExists")
        Assertions.assertTrue(multiQueue.add(message))

        Assertions.assertThrows(DuplicateMessageException::class.java)
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

        val message = QueueMessage("A test value", "testRemove")

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
        val messageThatDoesntExist = QueueMessage(Payload("some Other data", 23, false, PayloadEnum.A), "testRemove_whenEntryDoesntExist")

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
        val subQueue = "testContains_whenEntryDoesntExist"
        val otherData = Payload("some Other data", 65, true, PayloadEnum.B)
        val messageThatDoesntExist = QueueMessage(otherData, subQueue)
        Assertions.assertFalse(multiQueue.contains(messageThatDoesntExist))
    }

    /**
     * Ensure that `true` is returned from [MultiQueue.contains] when the entry does exist.
     */
    @Test
    fun testContains_whenEntryExists()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage(0x52347, "testContains_whenEntryExists")

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
        val message = QueueMessage(0x5234, "testContains_whenMetadataPropertiesAreSet")

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())

        Assertions.assertTrue(multiQueue.contains(message))
        message.assignedTo = "Instance_11242"
        Assertions.assertTrue(multiQueue.contains(message))
    }

    /**
     * Ensure that `false` is returned from [MultiQueue.contains] when the input object is `null`.
     */
    @Test
    fun testContains_null()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        Assertions.assertFalse(multiQueue.contains(null))
    }

    /**
     * Ensuring that only the [InMemoryMultiQueue] will auto increment the index as its retrieved but others will not
     * and [SqlMultiQueue] will always return [Optional.empty].
     */
    @Test
    fun testGetNextQueueIndex_doesNotIncrement()
    {
        val subQueue = "testGetNextQueueIndex_doesNotIncrement"
        if (multiQueue is SqlMultiQueue)
        {
            Assertions.assertTrue(multiQueue.getNextSubQueueIndex(subQueue).isEmpty)
        }
        else if (multiQueue is InMemoryMultiQueue)
        {
            Assertions.assertEquals(1, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(2, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(3, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(4, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(5, multiQueue.getNextSubQueueIndex(subQueue).get())

            multiQueue.clearSubQueue(subQueue)
            Assertions.assertEquals(1, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(2, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(3, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(4, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(5, multiQueue.getNextSubQueueIndex(subQueue).get())

            multiQueue.clear()
            Assertions.assertEquals(1, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(2, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(3, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(4, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(5, multiQueue.getNextSubQueueIndex(subQueue).get())
        }
        else
        {
            Assertions.assertTrue(multiQueue.getNextSubQueueIndex(subQueue).isPresent)
            Assertions.assertEquals(1, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(1, multiQueue.getNextSubQueueIndex(subQueue).get())
            Assertions.assertEquals(1, multiQueue.getNextSubQueueIndex(subQueue).get())
        }
    }

    /**
     * Ensure that [MultiQueue.getNextSubQueueIndex] starts at `1` and increments properly as called once entries are added.
     */
    @Test
    fun testGetNextQueueIndex_withMessages()
    {
        Assertions.assertTrue(multiQueue.isEmpty())

        val subQueue1 = "testGetNextQueueIndex_reInitialise1"
        val subQueue2 = "testGetNextQueueIndex_reInitialise2"

        val list1 = listOf(QueueMessage(81273648, subQueue1), QueueMessage("test test test", subQueue1), QueueMessage(false, subQueue1))
        val list2 = listOf(QueueMessage("test", subQueue2), QueueMessage(123, subQueue2))
        Assertions.assertTrue(multiQueue.addAll(list1))
        Assertions.assertTrue(multiQueue.addAll(list2))

        if (multiQueue is SqlMultiQueue)
        {
            Assertions.assertTrue(multiQueue.getNextSubQueueIndex(subQueue1).isEmpty)
            Assertions.assertTrue(multiQueue.getNextSubQueueIndex(subQueue2).isEmpty)
        }
        else if (multiQueue is InMemoryMultiQueue)
        {
            Assertions.assertEquals((list1.size + 1).toLong(), multiQueue.getNextSubQueueIndex(subQueue1).get())
            Assertions.assertEquals((list2.size + 1).toLong(), multiQueue.getNextSubQueueIndex(subQueue2).get())
        }
        else if (multiQueue is MongoMultiQueue)
        {
            Assertions.assertEquals((list1.size + list2.size + 1).toLong(), multiQueue.getNextSubQueueIndex(subQueue1).get())
            Assertions.assertEquals((list1.size + list2.size + 1).toLong(), multiQueue.getNextSubQueueIndex(subQueue2).get())
        }
        else
        {
            Assertions.assertEquals((list1.size + 1).toLong(), multiQueue.getNextSubQueueIndex(subQueue1).get())
            Assertions.assertEquals((list2.size + 1).toLong(), multiQueue.getNextSubQueueIndex(subQueue2).get())
        }
    }

    /**
     * Ensure [MultiQueue.getSubQueue] returns the list of [QueueMessage]s always ordered by their [QueueMessage.id].
     *
     * This also ensures they are assigned the `id` in the order they are enqueued.
     */
    @Test
    fun testGetqueue_ordered()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testGetqueue_ordered"

        val list = listOf(QueueMessage(81248, subQueue), QueueMessage("test data", subQueue), QueueMessage(false, subQueue))
        Assertions.assertTrue(multiQueue.addAll(list))
        val queue = multiQueue.getSubQueue(subQueue)
        Assertions.assertEquals(list.size, queue.size)
        var previousIndex: Long? = null
        list.zip(queue).forEach { pair ->
            Assertions.assertEquals(pair.first.uuid, pair.second.uuid)
            Assertions.assertNotNull(pair.second.id)
            if (previousIndex == null)
            {
                previousIndex = pair.second.id
            }
            else
            {
                Assertions.assertTrue(previousIndex!! < pair.second.id!!)
            }
        }
    }

    /**
     * Ensure [MultiQueue.getSubQueue] returns the list of [QueueMessage]s always ordered by their [QueueMessage.id].
     * Even when messages are changed and re-enqueued we need to make sure the returned message order is retained.
     */
    @Test
    fun testGetqueue_reordered()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testGetqueue_reordered"

        val list = listOf(QueueMessage(81248, subQueue), QueueMessage("test data", subQueue), QueueMessage(false, subQueue))
        Assertions.assertTrue(multiQueue.addAll(list))

        // Force an object change, which for some mechanisms would re-enqueue it at the end
        // We will re-retrieve the queue and ensure they are in order to test that the ordering is correct
        // even after the object is changed
        var queue = multiQueue.getSubQueue(subQueue)
        Assertions.assertEquals(list.size, queue.size)
        val firstMessage = queue.first()
        Assertions.assertEquals(list[0].uuid, firstMessage.uuid)
        val newData = "some test"
        firstMessage.payload = newData
        multiQueue.persistMessage(firstMessage)

        queue = multiQueue.getSubQueue(subQueue)
        var previousIndex: Long? = null
        list.zip(queue).forEach { pair ->
            Assertions.assertEquals(pair.first.uuid, pair.second.uuid)
            Assertions.assertNotNull(pair.second.id)
            if (previousIndex == null)
            {
                Assertions.assertEquals(newData, pair.second.payload)
                previousIndex = pair.second.id
            }
            else
            {
                Assertions.assertTrue(previousIndex!! < pair.second.id!!)
            }
        }
    }

    /**
     * Ensure that calls to [MultiQueue.getSubQueue] with a [MultiQueueAuthenticator.getReservedSubQueues] as an
     * argument will throw [IllegalSubQueueIdentifierException].
     */
    @Test
    fun testGetqueue_reservedSubQueue()
    {
        doWithRestrictedMode(RestrictionMode.HYBRID) {
            authenticator.getReservedSubQueues().forEach { reservedSubQueueIdentifier ->
                Assertions.assertThrows(IllegalSubQueueIdentifierException::class.java) {
                    multiQueue.getSubQueue(reservedSubQueueIdentifier)
                }
            }
        }
    }

    /**
     * Ensure that all elements are added, and contained and removed via the provided [Collection].
     */
    @Test
    fun testAddAll_containsAll_removeAll()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val list = listOf(QueueMessage(81273648, "testAddAll_containsAll_removeAll"), QueueMessage("test test test", "testAddAll_containsAll_removeAll"))
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
     * Test the [MultiQueue.addAll] to ensure that if a duplicate element is added that `false` is returned to indicate that not all the provided elements were added to the queue.
     */
    @Test
    fun testAddAll_throwsDuplicateException()
    {
        val list = listOf(QueueMessage(81273648, "testAddAll_throwsDuplicateException"), QueueMessage("test test test", "testAddAll_throwsDuplicateException"))
        Assertions.assertTrue(multiQueue.add(list[1]))
        Assertions.assertFalse(multiQueue.addAll(list))
        Assertions.assertEquals(list.size, multiQueue.size)
    }

    /**
     * Ensure that `null` is returned when there are no elements in the [MultiQueue] for the specific queue.
     * Otherwise, if it does exist make sure that the correct entry is returned and that it is removed.
     */
    @Test
    fun testPollForSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage(Payload("poll for type", 89, true, PayloadEnum.B), "testPollForSubQueue")

        Assertions.assertFalse(multiQueue.pollSubQueue(message.subQueue).isPresent)
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        val polledMessage = multiQueue.pollSubQueue(message.subQueue).get()
        Assertions.assertEquals(message, polledMessage)
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    /**
     * Ensure that `null` is returned when there are no elements in the [MultiQueue] for the specific queue.
     * Otherwise, if it does exist make sure that the correct entry is returned.
     */
    @Test
    fun testPeekForSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val message = QueueMessage(Payload("peek for type", 1121, false, PayloadEnum.C), "testPeekForSubQueue")

        Assertions.assertFalse(multiQueue.peekSubQueue(message.subQueue).isPresent)
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        val peekedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(message, peekedMessage)
        Assertions.assertFalse(multiQueue.isEmpty())
    }

    /**
     * Ensure that [MultiQueue.isEmptySubQueue] operates as expected when entries exist and don't exist for a specific sub-queue.
     */
    @Test
    fun testIsEmptyForSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())

        val subQueue = "testIsEmptyForSubQueue"
        val data = "test data"
        val message = QueueMessage(data, subQueue)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertFalse(multiQueue.isEmpty())
        Assertions.assertFalse(multiQueue.isEmptySubQueue(subQueue))
        Assertions.assertTrue(multiQueue.isEmptySubQueue("testIsEmptyForSubQueue-another"))
    }

    /**
     * Ensure that only the specific entries are removed when [MultiQueue.clearSubQueueInternal] is called.
     */
    @Test
    fun testClearForSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testClearForSubQueue"
        val list = listOf(QueueMessage(81273648, subQueue), QueueMessage("test test test", subQueue))
        Assertions.assertTrue(multiQueue.addAll(list))

        val singleEntrySubQueue = "testClearForSubQueue2"
        val message = QueueMessage("test message", singleEntrySubQueue)
        Assertions.assertTrue(multiQueue.add(message))

        Assertions.assertEquals(3, multiQueue.size)
        multiQueue.clearSubQueueInternal(subQueue)
        Assertions.assertEquals(1, multiQueue.size)
        multiQueue.clearSubQueueInternal(singleEntrySubQueue)
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    /**
     * Ensure that no change is made when the specific sub-queue has no entries.
     */
    @Test
    fun testClearSubQueue_DoesNotExist()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testClearSubQueue_DoesNotExist"
        multiQueue.clearSubQueueInternal(subQueue)
        Assertions.assertTrue(multiQueue.isEmpty())
    }

    /**
     * Ensure that the correct entries are retained and that the correct `Boolean` value is returned.
     */
    @Test
    fun testRetainAll()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testRetainAll1"
        val subQueue2 = "testRetainAll2"
        val data = Payload("some payload", 1, true, PayloadEnum.A)
        val data2 = Payload("some more data", 2, false, PayloadEnum.B)
        val list = listOf(QueueMessage(data, subQueue), QueueMessage(data, subQueue2), QueueMessage(data2, subQueue), QueueMessage(data2, subQueue2))

        Assertions.assertTrue(multiQueue.addAll(list))
        Assertions.assertEquals(4, multiQueue.size)

        val toRetain = ArrayList<QueueMessage>()
        toRetain.addAll(list.subList(0, 2))
        Assertions.assertEquals(2, toRetain.size)
        // No elements of this type to cover all branches of code
        val subQueue3 = "testRetainAll3"
        val message3 = QueueMessage(Payload("data 3", 3, false, PayloadEnum.C), subQueue3)
        toRetain.add(message3)
        Assertions.assertEquals(3, toRetain.size)

        Assertions.assertTrue(multiQueue.retainAll(toRetain))
        Assertions.assertEquals(2, multiQueue.size)
        Assertions.assertTrue(multiQueue.contains(list[0]))
        Assertions.assertTrue(multiQueue.contains(list[1]))

        Assertions.assertFalse(multiQueue.contains(list[2]))
        Assertions.assertFalse(multiQueue.contains(list[3]))
    }

    /**
     * Test [MultiQueue.persistMessage] to ensure that the changes are actually persisted to the stored message.
     *
     * There is a special case for the [InMemoryMultiQueue] where the change is persisted immediately since its all using referenced objects, otherwise
     * the other mechanisms should work the same way and require this method to be called before the changes are persisted.
     */
    @Test
    fun testPersistMessage()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testPersistMessage"
        val data = Payload("some payload", 1, true, PayloadEnum.A)
        val data2 = Payload("some more data", 2, false, PayloadEnum.B)

        val message = QueueMessage(data, subQueue)
        Assertions.assertTrue(multiQueue.add(message))

        val persistedMessage = multiQueue.peekSubQueue(message.subQueue)
        Assertions.assertTrue(persistedMessage.isPresent)
        val messageToUpdate = persistedMessage.get()
        Assertions.assertEquals(message, messageToUpdate)

        messageToUpdate.payload = data2

        // Since the InMemoryMultiQueue works off object references, the data will actually be updated in place, other mechanisms
        // that are backed by other mechanisms will need to explicitly persist the change before it is reflected
        if (multiQueue is InMemoryMultiQueue)
        {
            Assertions.assertEquals(message, persistedMessage.get())
        }
        else
        {
            Assertions.assertNotEquals(message.payload, persistedMessage.get().payload)
        }

        multiQueue.persistMessage(messageToUpdate)

        val reRetrievedMessage = multiQueue.peekSubQueue(message.subQueue)
        Assertions.assertTrue(reRetrievedMessage.isPresent)
        Assertions.assertEquals(messageToUpdate, reRetrievedMessage.get())
    }

    /**
     * Test [MultiQueue.persistMessage] when the incoming message has a null `id`.
     * A [MessageUpdateException] will be thrown for all [MultiQueue] except the [InMemoryMultiQueue].
     */
    @Test
    fun testPersistMessage_messageHasNullID()
    {
        val message = QueueMessage("payload", "testPersistMessage_messageHasNullID")
        Assertions.assertNull(message.id)

        if (multiQueue !is InMemoryMultiQueue)
        {
            // If it's an in-memory queue there will be no exception thrown
            Assertions.assertThrows(MessageUpdateException::class.java) {
                multiQueue.persistMessage(message)
            }
        }
    }

    /**
     * Test [MultiQueue.getAssignedMessagesInSubQueue] returns only messages with a non-null [QueueMessage.assignedTo] property.
     */
    @Test
    fun testGetAssignedMessagesForSubQueue_noAssignedTo()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testGetAssignedMessagesForSubQueue_noAssignedTo"
        val message = QueueMessage(Payload("some payload", 1, true, PayloadEnum.A), subQueue)
        val message2 = QueueMessage(Payload("some more data", 2, false, PayloadEnum.B), subQueue)
        val message3 = QueueMessage(Payload("some more data data", 3, false, PayloadEnum.C), subQueue)

        // Assign message 1
        val assignedTo = "me"
        message.assignedTo = assignedTo
        Assertions.assertNull(message3.assignedTo)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))

        // Assign and update message 2, to check that even if its changed and re-enqueued that it retains the correct order
        message2.assignedTo = assignedTo
        multiQueue.persistMessage(message2)

        // Ensure all messages are in the queue
        val messagesInSubQueue = multiQueue.getSubQueue(subQueue)
        Assertions.assertEquals(3, messagesInSubQueue.size)

        // Check only messages 1 and 2 are returned in the assigned queue
        val assignedMessages = multiQueue.getAssignedMessagesInSubQueue(subQueue, null)
        Assertions.assertEquals(2, assignedMessages.size)

        val list = ArrayList<QueueMessage>()
        list.addAll(assignedMessages)
        Assertions.assertTrue(list.contains(message))
        Assertions.assertTrue(list.contains(message2))
        Assertions.assertFalse(list.contains(message3))
    }

    /**
     * Test [MultiQueue.getAssignedMessagesInSubQueue] returns only messages with the matching [QueueMessage.assignedTo] property.
     */
    @Test
    fun testGetAssignedMessagesForSubQueue_withAssignedTo()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testGetAssignedMessagesForSubQueue_withAssignedTo"
        val message = QueueMessage(Payload("some payload", 1, true, PayloadEnum.A), subQueue)
        val message2 = QueueMessage(Payload("some more data", 2, false, PayloadEnum.B), subQueue)
        val message3 = QueueMessage(Payload("some more data data", 3, false, PayloadEnum.C), subQueue)
        val message4 = QueueMessage(Payload("some more data data data", 4, false, PayloadEnum.A), subQueue)

        // Assign message 1, 2 and 3
        val assignedTo = "me"
        val assignedTo2 = "me2"
        message.assignedTo = assignedTo
        message3.assignedTo = assignedTo2
        Assertions.assertNull(message4.assignedTo)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))
        Assertions.assertTrue(multiQueue.add(message4))

        // Assign and update message 2, to check that even if its changed and re-enqueued that it retains the correct order
        message2.assignedTo = assignedTo
        multiQueue.persistMessage(message2)

        // Ensure all messages are in the queue
        val messagesInSubQueue = multiQueue.getSubQueue(subQueue)
        Assertions.assertEquals(4, messagesInSubQueue.size)

        // Check only messages 1 and 2 are assigned to 'assignedTo'
        val assignedMessages = multiQueue.getAssignedMessagesInSubQueue(subQueue, assignedTo)
        Assertions.assertEquals(2, assignedMessages.size)

        val list = ArrayList<QueueMessage>()
        list.addAll(assignedMessages)
        Assertions.assertTrue(list.contains(message))
        Assertions.assertTrue(list.contains(message2))
        Assertions.assertFalse(list.contains(message3))
        Assertions.assertFalse(list.contains(message4))
    }

    /**
     * Test [MultiQueue.getUnassignedMessagesInSubQueue] returns only messages with a `null` [QueueMessage.assignedTo] property.
     */
    @Test
    fun testGetUnassignedMessagesForSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testGetUnassignedMessagesForSubQueue"
        val message = QueueMessage(Payload("some payload", 1, true, PayloadEnum.A), subQueue)
        val message2 = QueueMessage(Payload("some more data", 2, false, PayloadEnum.B), subQueue)
        val message3 = QueueMessage(Payload("some more data data", 3, false, PayloadEnum.C), subQueue)
        val message4 = QueueMessage(Payload("some more data data data", 4, true, PayloadEnum.A), subQueue)

        val assignedTo = "you"
        message.assignedTo = assignedTo
        message2.assignedTo = assignedTo
        message3.assignedTo = assignedTo
        Assertions.assertNull(message4.assignedTo)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))
        Assertions.assertTrue(multiQueue.add(message4))

        // Now un-assign message 3 to make sure persisted messages are ordered properly after they are changed
        message3.assignedTo = null
        multiQueue.persistMessage(message3)

        // Ensure all messages are in the queue
        val messagesInSubQueue = multiQueue.getSubQueue(subQueue)
        Assertions.assertEquals(4, messagesInSubQueue.size)

        // Check only messages 3 and 4 are returned in the unassigned queue
        val assignedMessages = multiQueue.getUnassignedMessagesInSubQueue(subQueue)
        Assertions.assertEquals(2, assignedMessages.size)

        val list = ArrayList<QueueMessage>()
        list.addAll(assignedMessages)
        Assertions.assertTrue(list.contains(message3))
        Assertions.assertTrue(list.contains(message4))
        Assertions.assertFalse(list.contains(message))
        Assertions.assertFalse(list.contains(message2))
    }

    /**
     * Test [MultiQueue.getOwnersAndKeysMapForSubQueue] to ensure that the provided map is populated properly with the correct entries
     * for the current [MultiQueue] state.
     */
    @Test
    fun testGetOwnersAndKeysMapForSubQueue()
    {
        val responseMap = HashMap<String, HashSet<String>>()

        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "test-get-owners-and-keys-map-for-subqueue"
        val subQueue2 = "test-get-owners-and-keys-map-for-subqueue2"
        val message = QueueMessage(Payload("some payload", 1, true, PayloadEnum.A), subQueue)
        val message2 = QueueMessage(Payload("some more data", 2, false, PayloadEnum.B), subQueue)
        val message3 = QueueMessage(Payload("some more data data", 3, false, PayloadEnum.C), subQueue2)
        val message4 = QueueMessage(Payload("some more data data data", 4, true, PayloadEnum.A), subQueue)

        val assignedTo = "assigned1"
        val assignedTo2 = "assigned2"
        message.assignedTo = assignedTo
        message2.assignedTo = assignedTo2
        message3.assignedTo = assignedTo
        message4.assignedTo = assignedTo

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))
        Assertions.assertTrue(multiQueue.add(message4))

        multiQueue.getOwnersAndKeysMapForSubQueue(subQueue, responseMap)

        Assertions.assertEquals(2, responseMap.keys.size)
        val listOfKeys = ArrayList<String>()
        listOfKeys.addAll(responseMap.keys)

        Assertions.assertTrue(listOfKeys.contains(assignedTo))
        Assertions.assertTrue(listOfKeys.contains(assignedTo2))

        val subQueuesForAssignedTo = responseMap[assignedTo]
        Assertions.assertEquals(1, subQueuesForAssignedTo!!.size)
        Assertions.assertEquals(subQueue, subQueuesForAssignedTo.iterator().next())

        val subQueuesForAssignedTo2 = responseMap[assignedTo2]
        Assertions.assertEquals(1, subQueuesForAssignedTo2!!.size)
        Assertions.assertEquals(subQueue, subQueuesForAssignedTo2.iterator().next())
    }

    /**
     * Test [MultiQueue.getOwnersAndKeysMap] with a specified sub-queue to ensure that the provided map is populated properly with the correct entries
     * for the current [MultiQueue] state.
     */
    @Test
    fun testGetOwnersAndKeysMap_inSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "testGetOwnersAndKeysMap_inSubQueue"
        val subQueue2 = "testGetOwnersAndKeysMap_inSubQueue2"
        val message = QueueMessage(Payload("some payload", 1, true, PayloadEnum.A), subQueue)
        val message2 = QueueMessage(Payload("some more data", 2, false, PayloadEnum.B), subQueue)
        val message3 = QueueMessage(Payload("some more data data", 3, false, PayloadEnum.C), subQueue2)
        val message4 = QueueMessage(Payload("some more data data data", 4, true, PayloadEnum.A), subQueue)

        val assignedTo = "assigned1"
        val assignedTo2 = "assigned2"
        message.assignedTo = assignedTo
        message2.assignedTo = assignedTo2
        message3.assignedTo = assignedTo
        message4.assignedTo = assignedTo

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))
        Assertions.assertTrue(multiQueue.add(message4))

        val responseMap = multiQueue.getOwnersAndKeysMap(subQueue)

        Assertions.assertEquals(2, responseMap.keys.size)
        val listOfKeys = responseMap.keys.toList()

        Assertions.assertTrue(listOfKeys.contains(assignedTo))
        Assertions.assertTrue(listOfKeys.contains(assignedTo2))

        val subQueuesForAssignedTo = responseMap[assignedTo]
        Assertions.assertEquals(1, subQueuesForAssignedTo!!.size)
        Assertions.assertEquals(subQueue, subQueuesForAssignedTo.iterator().next())

        val subQueuesForAssignedTo2 = responseMap[assignedTo2]
        Assertions.assertEquals(1, subQueuesForAssignedTo2!!.size)
        Assertions.assertEquals(subQueue, subQueuesForAssignedTo2.iterator().next())
    }

    /**
     * Test [MultiQueue.getOwnersAndKeysMap] without a specified sub-queue to ensure that the provided map is populated properly with the correct entries
     * for the current [MultiQueue] state.
     */
    @Test
    fun testGetOwnersAndKeysMap_notInSubQueue()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val subQueue = "test-get-owners-and-keys-map"
        val subQueue2 = "test-get-owners-and-keys-map2"
        val subQueue3 = "test-get-owners-and-keys-map3"
        val message = QueueMessage(Payload("some payload", 1, true, PayloadEnum.A), subQueue)
        val message2 = QueueMessage(Payload("some more data", 2, false, PayloadEnum.B), subQueue)
        val message3 = QueueMessage(Payload("some more data data", 3, false, PayloadEnum.C), subQueue2)
        val message4 = QueueMessage(Payload("some more data data data", 4, true, PayloadEnum.A), subQueue)
        val message5 = QueueMessage(Payload("just data", 5, true, PayloadEnum.C), subQueue3)
        val message6 = QueueMessage(Payload("just more data", 6, false, PayloadEnum.B), subQueue2)
        val message7 = QueueMessage(Payload("just more and more data", 7, false, PayloadEnum.A), subQueue)

        val assignedTo = "assigned1"
        val assignedTo2 = "assigned2"
        val assignedTo3 = "assigned3"
        message.assignedTo = assignedTo
        message2.assignedTo = assignedTo
        message3.assignedTo = assignedTo2
        message4.assignedTo = assignedTo2
        message5.assignedTo = assignedTo3
        message6.assignedTo = assignedTo3

        Assertions.assertNull(message7.assignedTo)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))
        Assertions.assertTrue(multiQueue.add(message4))
        Assertions.assertTrue(multiQueue.add(message5))
        Assertions.assertTrue(multiQueue.add(message6))

        val responseMap = multiQueue.getOwnersAndKeysMap(null)

        Assertions.assertEquals(3, responseMap.keys.size)
        val listOfKeys = responseMap.keys.toList()

        Assertions.assertTrue(listOfKeys.contains(assignedTo))
        Assertions.assertTrue(listOfKeys.contains(assignedTo2))
        Assertions.assertTrue(listOfKeys.contains(assignedTo3))

        val subQueuesForAssignedTo = responseMap[assignedTo]!!.toList()
        Assertions.assertEquals(1, subQueuesForAssignedTo.size)
        Assertions.assertTrue(subQueuesForAssignedTo.contains(subQueue))

        val subQueuesForAssignedTo2 = responseMap[assignedTo2]!!.toList()
        Assertions.assertEquals(2, subQueuesForAssignedTo2.size)
        Assertions.assertTrue(subQueuesForAssignedTo2.contains(subQueue))
        Assertions.assertTrue(subQueuesForAssignedTo2.contains(subQueue2))

        val subQueuesForAssignedTo3 = responseMap[assignedTo3]!!.toList()
        Assertions.assertEquals(2, subQueuesForAssignedTo3.size)
        Assertions.assertTrue(subQueuesForAssignedTo3.contains(subQueue2))
        Assertions.assertTrue(subQueuesForAssignedTo3.contains(subQueue3))
    }

    /**
     * Test [MultiQueue.getMessageByUUID] to ensure that the corresponding message is returned.
     */
    @Test
    fun testGetMessageByUUID_matchingMessage()
    {
        val subQueue = "testGetMessageByUUID_matchingMessage"
        val message = QueueMessage(Payload("some payload", 1, true, PayloadEnum.A), subQueue)
        Assertions.assertTrue(multiQueue.add(message))

        val retrievedMessage = multiQueue.getMessageByUUID(message.uuid)
        Assertions.assertTrue(retrievedMessage.isPresent)
        Assertions.assertEquals(message, retrievedMessage.get())
        Assertions.assertEquals(message.payload, retrievedMessage.get().payload)
    }

    /**
     * Test [MultiQueue.getMessageByUUID] to ensure that [Optional.EMPTY] is returned when the corresponding message does not exist.
     */
    @Test
    fun testGetMessageByUUID_messageDoesNotExist()
    {
        Assertions.assertTrue(multiQueue.isEmpty())
        val uuid = UUID.randomUUID().toString()
        Assertions.assertEquals(Optional.empty<QueueMessage>(), multiQueue.getMessageByUUID(uuid))
    }

    /**
     * Test [MultiQueue.performHealthCheck] to ensure no `HealthCheckFailureException`s are thrown when this is called while the storage mechanism is running correctly.
     */
    @Test
    fun testPerformHealthCheck_successfulCheck()
    {
        multiQueue.performHealthCheck()
    }

    /**
     * Ensure that we cannot add a new [QueueMessage] with [QueueMessage.subQueue] set to any of the [MultiQueueAuthenticator.getReservedSubQueues] entries.
     */
    @Test
    fun testAddReservedSubQueue()
    {
        doWithRestrictedMode(RestrictionMode.RESTRICTED) {
            authenticator.getReservedSubQueues().forEach { reservedSubQueueIdentifier ->
                val message = QueueMessage("Data", reservedSubQueueIdentifier)
                Assertions.assertThrows(IllegalSubQueueIdentifierException::class.java) {
                    multiQueue.add(message)
                }
            }
        }
    }

    /**
     * Ensure that even we have a restricted queue entry registered, when [MultiQueue.keys] is called, the entry
     * is removed and not returned.
     */
    @Test
    fun testKeysWithReservedSubQueueUsage()
    {
        doWithRestrictedMode(RestrictionMode.HYBRID) {
            var keys = multiQueue.keys()
            authenticator.getReservedSubQueues().forEach { reservedSubQueueIdentifier ->
                Assertions.assertFalse(keys.contains(reservedSubQueueIdentifier))
            }

            val restrictedSubQueue = "testKeysWithReservedSubQueueUsage"
            authenticator.addRestrictedEntry(restrictedSubQueue)

            keys = multiQueue.keys()
            authenticator.getReservedSubQueues().forEach { reservedSubQueueIdentifier ->
                Assertions.assertFalse(keys.contains(reservedSubQueueIdentifier))
            }

            // Need to clear the restricted queue otherwise it affects other redis tests if they are not in a "non-None" auth state
            authenticator.clearRestrictedSubQueues()
        }
    }

    /**
     * Perform the provided [function] with the [RestrictionMode] set to [restrictionMode].
     * Once completed the [RestrictionMode] will be set back to its initial value.
     *
     * @param restrictionMode the [RestrictionMode] to be set while the [function] is being called
     * @param function the function to call with the provided [RestrictionMode] being active
     * @return `T` the result of the [function]
     */
    private fun <T> doWithRestrictedMode(restrictionMode: RestrictionMode, function: Supplier<T>): T
    {
        val previousRestrictedMode = authenticator.getRestrictionMode()
        authenticator.setRestrictionMode(restrictionMode)

        try
        {
            return function.get()
        }
        finally
        {
            authenticator.setRestrictionMode(previousRestrictedMode)
        }
    }

    /**
     * Ensure that all applicable methods throw an [UnsupportedOperationException].
     */
    @Test
    fun testUnsupportedMethods()
    {
        assertAll( "Unsupported methods",
            {
                Assertions.assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.peek()
                }
            },
            {
                Assertions.assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.offer(QueueMessage(Payload("test data", 13, false, PayloadEnum.C), "test sub-queue"))
                }
            },
            {
                Assertions.assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.element()
                }
            },
            {
                Assertions.assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.poll()
                }
            },
            {
                Assertions.assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.remove()
                }
            },
            {
                Assertions.assertThrows(UnsupportedOperationException::class.java)
                {
                    multiQueue.iterator()
                }
            }
        )
    }
}
