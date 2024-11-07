package au.kilemon.messagequeue.rest.controller

import au.kilemon.messagequeue.authentication.RestrictionMode
import au.kilemon.messagequeue.authentication.authenticator.MultiQueueAuthenticator
import au.kilemon.messagequeue.authentication.token.JwtTokenProvider
import au.kilemon.messagequeue.configuration.QueueConfiguration
import au.kilemon.messagequeue.filter.CorrelationIdFilter
import au.kilemon.messagequeue.filter.JwtAuthenticationFilter
import au.kilemon.messagequeue.logging.LoggingConfiguration
import au.kilemon.messagequeue.message.QueueMessage
import au.kilemon.messagequeue.queue.MultiQueue
import au.kilemon.messagequeue.rest.model.Payload
import au.kilemon.messagequeue.rest.model.PayloadEnum
import au.kilemon.messagequeue.rest.response.KeysResponse
import au.kilemon.messagequeue.rest.response.MessageListResponse
import au.kilemon.messagequeue.rest.response.MessageResponse
import au.kilemon.messagequeue.rest.response.OwnersMapResponse
import au.kilemon.messagequeue.settings.MessageQueueSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

/**
 * A test class for the [MessageQueueController].
 * Providing tests of the context and endpoint validation/handling itself.
 *
 * @author github.com/Kilemonn
 */
@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [MessageQueueController::class], properties = ["${MessageQueueSettings.STORAGE_MEDIUM}=IN_MEMORY"])
@Import(*[QueueConfiguration::class, LoggingConfiguration::class])
class MessageQueueControllerTest
{
    /**
     * A [TestConfiguration] for the outer [MessageQueueControllerTest] class.
     *
     * @author github.com/Kilemonn
     */
    @TestConfiguration
    open class TestConfig
    {
        @Bean
        open fun getSettings(): MessageQueueSettings
        {
            return MessageQueueSettings()
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @SpyBean
    private lateinit var authenticator: MultiQueueAuthenticator

    @SpyBean
    private lateinit var multiQueue: MultiQueue

    private val gson: Gson = Gson()

    /**
     * [BeforeEach] method to run [MultiQueue.clear] and ensure that [MultiQueue.isEmpty] returns `true` at the
     * beginning of each test.
     */
    @BeforeEach
    fun setUp()
    {
        multiQueue.clear()
        Assertions.assertTrue(multiQueue.isEmpty())

        authenticator.clearRestrictedSubQueues()
        Assertions.assertTrue(authenticator.getRestrictedSubQueueIdentifiers().isEmpty())
    }

    /**
     * Test [MessageQueueController.getSubQueueInfo] to ensure the correct information is returned for the specified
     * `sub-queue`.
     */
    @Test
    fun testGetSubQueueInfo()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val subQueue = "testGetSubQueueInfo"
        Assertions.assertEquals(0, multiQueue.getSubQueue(subQueue).size)
        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_TYPE + "/" + subQueue)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("0"))

        val message = createQueueMessage(subQueue = subQueue)
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertEquals(1, multiQueue.getSubQueue(subQueue).size)

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_TYPE + "/" + subQueue)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("1"))
    }

    /**
     * Test [MessageQueueController.getAllQueueInfo] to ensure that information for all `sub-queue`s is returned
     * when no `sub-queue` is specified.
     */
    @Test
    fun testGetAllSubQueueInfo()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_TYPE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("0"))

        val message = createQueueMessage(subQueue = "testGetAllSubQueueInfo1")
        val message2 = createQueueMessage(subQueue = "testGetAllSubQueueInfo2")

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))

        Assertions.assertEquals(1, multiQueue.getSubQueue(message.subQueue).size)
        Assertions.assertEquals(1, multiQueue.getSubQueue(message2.subQueue).size)
        Assertions.assertEquals(2, multiQueue.size)

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_TYPE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("2"))
    }

    /**
     * Test [MessageQueueController.getEntry] to ensure that [HttpStatus.OK] and the correct [QueueMessage] is returned
     * as the response.
     */
    @Test
    fun testGetEntry()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testGetEntry")

        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(message.payload, deserialisedPayload)
        Assertions.assertNull(messageResponse.message.assignedTo)
        Assertions.assertEquals(message.subQueue, messageResponse.message.subQueue)
        Assertions.assertNotNull(messageResponse.message.uuid)

        Assertions.assertNull(messageResponse.message.payloadBytes)
        Assertions.assertNull(messageResponse.message.id)
    }

    /**
     * Test [MessageQueueController.getEntry] to ensure that [HttpStatus.NO_CONTENT] is returned when a [UUID] that
     * does not exist is provided.
     */
    @Test
    fun testGetEntry_ResponseBody_NotExists()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val uuid = "invalid-not-found-uuid"
        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Ensure that in [RestrictionMode.HYBRID] mode, when we restrict the sub-queue that we can no longer
     * get entries from that specific sub-queue, other sub-queues are still accessible without a token.
     */
    @Test
    fun testGetEntry_usingHybridMode_withRestrictedSubQueue()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val entries = initialiseMapWithEntries()

        val subQueue1 = entries.second[0]
        val message1 = entries.first[0]
        val subQueue1Token = jwtTokenProvider.createTokenForSubQueue(subQueue1)
        Assertions.assertTrue(subQueue1Token.isPresent)
        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueue1))
        Assertions.assertTrue(authenticator.isRestricted(subQueue1))

        // Make sure we cannot access without a token
        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message1.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        // Checking entry is retrieve with provided token
        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message1.uuid)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER,  "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${subQueue1Token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(message1.payload, deserialisedPayload)
        Assertions.assertNull(messageResponse.message.assignedTo)
        Assertions.assertEquals(message1.subQueue, messageResponse.message.subQueue)
        Assertions.assertNotNull(messageResponse.message.uuid)

        val subQueue2 = entries.second[1]
        val message2 = entries.first[1]
        Assertions.assertFalse(authenticator.isRestricted(subQueue2))

        // Check un-restricted entry is still accessible without a token
        val mvcResult2: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message2.uuid)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER,  "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${subQueue1Token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse2 = gson.fromJson(mvcResult2.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload2 = gson.fromJson(gson.toJson(messageResponse2.message.payload), Payload::class.java)
        Assertions.assertEquals(message2.payload, deserialisedPayload2)
        Assertions.assertNull(messageResponse2.message.assignedTo)
        Assertions.assertEquals(message2.subQueue, messageResponse2.message.subQueue)
        Assertions.assertNotNull(messageResponse2.message.uuid)
    }

    /**
     * Calling create with provided [QueueMessage.assignedTo] and [QueueMessage.uuid] to
     * ensure they are set correctly in the returned [MessageResponse].
     */
    @Test
    fun testCreateQueueEntry_withProvidedDefaults()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testCreateQueueEntry_withProvidedDefaults", assignedTo = "user-1")

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(message.payload, deserialisedPayload)
        Assertions.assertEquals(message.assignedTo, messageResponse.message.assignedTo)
        Assertions.assertEquals(message.subQueue, messageResponse.message.subQueue)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        val createdMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(message.assignedTo, createdMessage.assignedTo)
        Assertions.assertEquals(message.subQueue, createdMessage.subQueue)
        Assertions.assertEquals(message.uuid, createdMessage.uuid)
    }

    /**
     * Calling create without [QueueMessage.assignedTo] and [QueueMessage.uuid] to
     * ensure they are initialised as expected when they are not provided by the caller.
     */
    @Test
    fun testCreateQueueEntry_withOutDefaults()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testCreateQueueEntry_withOutDefaults")

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)

        val deserialisedPayload = gson.fromJson(gson.toJson(messageResponse.message.payload), Payload::class.java)
        Assertions.assertEquals(message.payload, deserialisedPayload)
        Assertions.assertNull(messageResponse.message.assignedTo)
        Assertions.assertEquals(message.subQueue, messageResponse.message.subQueue)
        Assertions.assertNotNull(messageResponse.message.uuid)

        val createdMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertNull(createdMessage.assignedTo)
        Assertions.assertEquals(message.subQueue, createdMessage.subQueue)
        Assertions.assertEquals(message.uuid, createdMessage.uuid)
    }

    /**
     * Test [MessageQueueController.createMessage] to ensure that [HttpStatus.CONFLICT] is returned if a message with
     * the same [UUID] already exists in the queue.
     */
    @Test
    fun testCreateEntry_Conflict()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testCreateEntry_Conflict")

        Assertions.assertTrue(multiQueue.add(message))

        mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isConflict)
    }

    /**
     * Calling create with a blank [QueueMessage.assignedTo] to make sure that [QueueMessage.assignedTo] is provided as
     * `null` in the response.
     */
    @Test
    fun testCreateQueueEntry_withBlankAssignedTo()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testCreateQueueEntry_withAssignedButNoAssignedTo")
        message.assignedTo = " "

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertNull(messageResponse.message.assignedTo)
    }

    /**
     * Ensure that in [RestrictionMode.HYBRID] mode, when we restrict the sub-queue that we can no longer
     * create entries for that specific sub-queue, other sub-queues can still have messages created without a token.
     */
    @Test
    fun testCreateEntry_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testCreateEntry_inHybridMode")

        Assertions.assertTrue(authenticator.addRestrictedEntry(message.subQueue))
        Assertions.assertTrue(authenticator.isRestricted(message.subQueue))

        val token = jwtTokenProvider.createTokenForSubQueue(message.subQueue)
        Assertions.assertTrue(token.isPresent)

        mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)

        val message2 = createQueueMessage(subQueue = "testCreateEntry_inHybridMode2")
        Assertions.assertFalse(authenticator.isRestricted(message2.subQueue))

        mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message2)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
    }

    /**
     * Test [MessageQueueController.getKeys] to ensure that all keys for existing entries are provided and exist within
     * the [MultiQueue].
     */
    @Test
    fun testGetKeys()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val entries = initialiseMapWithEntries()

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val keys = gson.fromJson(mvcResult.response.contentAsString, KeysResponse::class.java)
        Assertions.assertFalse(keys.keys.isEmpty())
        Assertions.assertEquals(entries.second.size, keys.keys.size)
        entries.second.forEach { subQueue -> Assertions.assertTrue(keys.keys.contains(subQueue)) }

        val mapKeys = multiQueue.keys(true)
        Assertions.assertFalse(mapKeys.isEmpty())
        Assertions.assertEquals(entries.second.size, mapKeys.size)
        entries.second.forEach { subQueue -> Assertions.assertTrue(mapKeys.contains(subQueue)) }
    }

    /**
     * Test [MessageQueueController.getKeys] to ensure that all keys are returned. Specifically when entries are added
     * and [RestParameters.INCLUDE_EMPTY] is set to `false`.
     */
    @Test
    fun testGetKeys_excludeEmpty()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val entries = initialiseMapWithEntries()
        Assertions.assertTrue(multiQueue.remove(entries.first[0]))
        Assertions.assertTrue(multiQueue.remove(entries.first[1]))

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.INCLUDE_EMPTY, "false"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val keys = gson.fromJson(mvcResult.response.contentAsString, KeysResponse::class.java)
        Assertions.assertFalse(keys.keys.isEmpty())
        Assertions.assertEquals(2, keys.keys.size)
        entries.second.subList(2, 3).forEach { subQueue -> Assertions.assertTrue(keys.keys.contains(subQueue)) }

        val mapKeys = multiQueue.keys(false)
        Assertions.assertFalse(mapKeys.isEmpty())
        Assertions.assertEquals(2, mapKeys.size)
        entries.second.subList(2, 3).forEach { subQueue -> Assertions.assertTrue(mapKeys.contains(subQueue)) }
    }

    /**
     * Test [MessageQueueController.getAll] to ensure that all entries are returned from all `sub-queues` when no
     * explicit `sub-queue` is provided.
     * This also checks the returned object has a `non-null` value in the payload since the `detailed` flag is set to
     * `true`.
     */
    @Test
    fun testGetAll()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val entries = initialiseMapWithEntries()
        val subQueue = entries.first[0].subQueue
        val detailed = true

        val mvcResult: MvcResult = mockMvc.perform(get("${MessageQueueController.MESSAGE_QUEUE_BASE_PATH}/${MessageQueueController.ENDPOINT_ALL}?${RestParameters.DETAILED}=$detailed")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val mapType = object : TypeToken<Map<String, List<QueueMessage>>>() {}.type
        val keys = gson.fromJson<Map<String, List<QueueMessage>>>(mvcResult.response.contentAsString, mapType)
        Assertions.assertNotNull(keys)
        Assertions.assertEquals(entries.second.size, keys.keys.size)
        entries.second.forEach { subQueueId -> Assertions.assertTrue(keys.keys.contains(subQueueId)) }
        keys.values.forEach { detailList -> Assertions.assertEquals(1, detailList.size) }

        Assertions.assertEquals(entries.first[0].removePayload(detailed).uuid, keys[subQueue]!![0].uuid)
        // Since we passed in true for the detailed flag, ensure the payload is equal
        val payloadObject = gson.fromJson(keys[subQueue]!![0].payload.toString(), Payload::class.java)
        Assertions.assertEquals(entries.first[0].payload, payloadObject)
        Assertions.assertEquals(entries.first[0].removePayload(detailed).assignedTo, keys[subQueue]!![0].assignedTo)
        Assertions.assertEquals(entries.first[0].removePayload(detailed).subQueue, keys[subQueue]!![0].subQueue)
    }

    /**
     * Test [MessageQueueController.getAll] to ensure that all entries are returned from the `sub-queue` when an
     * explicit `sub-queue` is provided.
     * This also checks the returned object has `null` in the payload since the `detailed` flag is not provided.
     */
    @Test
    fun testGetAll_SpecificSubQueue()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val entries = initialiseMapWithEntries()
        val subQueue = entries.first[0].subQueue
        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ALL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val mapType = object : TypeToken<Map<String, List<QueueMessage>>>() {}.type
        val keys = gson.fromJson<Map<String, List<QueueMessage>>>(mvcResult.response.contentAsString, mapType)
        Assertions.assertNotNull(keys)
        Assertions.assertEquals(1, keys.keys.size)
        Assertions.assertTrue(keys.keys.contains(subQueue))
        keys.values.forEach { detailList -> Assertions.assertEquals(1, detailList.size) }
        Assertions.assertEquals(entries.first[0].removePayload(false).uuid, keys[subQueue]!![0].uuid)
        // Since we did not pass a detailed flag value, ensure the payload is a placeholder value
        Assertions.assertEquals("***", entries.first[0].removePayload(false).payload)
        Assertions.assertEquals(entries.first[0].removePayload(false).assignedTo, keys[subQueue]!![0].assignedTo)
        Assertions.assertEquals(entries.first[0].removePayload(false).subQueue, keys[subQueue]!![0].subQueue)
    }

    /**
     * Ensure then when in [RestrictionMode.HYBRID] and [MessageQueueController.getAll] is called
     * that restricted queues are not included until a valid token is provided.
     */
    @Test
    fun testGetAll_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val subQueue = "testGetAll_inHybridMode"
        val messages = listOf(createQueueMessage(subQueue), createQueueMessage(subQueue))
        messages.forEach { message -> Assertions.assertTrue(multiQueue.add(message)) }

        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(authenticator.isRestricted(subQueue))

        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        val entries = initialiseMapWithEntries()
        entries.second.forEach { subQueueId -> Assertions.assertFalse(authenticator.isRestricted(subQueueId)) }
        val detailed = true

        // Ensure the message in the restricted sub-queue are not returned
        var mvcResult: MvcResult = mockMvc.perform(get("${MessageQueueController.MESSAGE_QUEUE_BASE_PATH}/${MessageQueueController.ENDPOINT_ALL}?${RestParameters.DETAILED}=$detailed")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val mapType = object : TypeToken<Map<String, List<QueueMessage>>>() {}.type
        var keys = gson.fromJson<Map<String, List<QueueMessage>>>(mvcResult.response.contentAsString, mapType)
        Assertions.assertNotNull(keys)
        Assertions.assertEquals(entries.second.size, keys.keys.size)

        Assertions.assertFalse(keys.keys.contains(subQueue))
        Assertions.assertNull(keys[subQueue])

        // After providing the token we should see the messages for the restricted queue
        mvcResult = mockMvc.perform(get("${MessageQueueController.MESSAGE_QUEUE_BASE_PATH}/${MessageQueueController.ENDPOINT_ALL}?${RestParameters.DETAILED}=$detailed")
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        keys = gson.fromJson(mvcResult.response.contentAsString, mapType)
        Assertions.assertNotNull(keys)
        Assertions.assertEquals(entries.second.size + 1, keys.keys.size)

        Assertions.assertTrue(keys.keys.contains(subQueue))
        Assertions.assertNotNull(keys[subQueue])
    }

    /**
     * Ensure then when in [RestrictionMode.HYBRID] and [MessageQueueController.getAll] is called
     * and all messages are requested for a restricted queue are not accessible unless a token is provided.
     */
    @Test
    fun testGetAll_SpecificSubQueue_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val subQueue = "testGetAll_SpecificSubQueue_inHybridMode"
        val messages = listOf(createQueueMessage(subQueue), createQueueMessage(subQueue))
        messages.forEach { message -> Assertions.assertTrue(multiQueue.add(message)) }

        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(authenticator.isRestricted(subQueue))

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ALL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ALL)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val mapType = object : TypeToken<Map<String, List<QueueMessage>>>() {}.type
        val keys = gson.fromJson<Map<String, List<QueueMessage>>>(mvcResult.response.contentAsString, mapType)
        Assertions.assertNotNull(keys)
        Assertions.assertEquals(1, keys.keys.size)

        Assertions.assertTrue(keys.keys.contains(subQueue))
        Assertions.assertNotNull(keys[subQueue])
        Assertions.assertEquals(2, keys[subQueue]!!.size)
    }

    /**
     * Test [MessageQueueController.getOwned] to ensure that no entries are returned when no [QueueMessage] are
     * assigned by the provided `assignedTo` parameter.
     */
    @Test
    fun testGetOwned_NoneOwned()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val entries = initialiseMapWithEntries()
        val assignedTo = "my-assigned-to-identifier"

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNED)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo)
            .param(RestParameters.SUB_QUEUE, entries.first[0].subQueue))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val owned = gson.fromJson<MessageListResponse>(mvcResult.response.contentAsString, MessageListResponse::class.java)
        Assertions.assertTrue(owned.messages.isEmpty())
    }

    /**
     * Test [MessageQueueController.getOwned] to ensure that all appropriate [QueueMessage] entries are returned when
     * the provided `assignedTo` parameter owns the existing [QueueMessage].
     */
    @Test
    fun testGetOwned_SomeOwned()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "my-assigned-to-identifier"
        val subQueue = "testGetOwned_SomeOwned"
        val message1 = createQueueMessage(assignedTo = assignedTo, subQueue = subQueue)
        val message2 = createQueueMessage(assignedTo = assignedTo, subQueue = subQueue)

        Assertions.assertTrue(multiQueue.add(message1))
        Assertions.assertTrue(multiQueue.add(message2))

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNED)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo)
            .param(RestParameters.SUB_QUEUE, subQueue))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val owned = gson.fromJson<MessageListResponse>(mvcResult.response.contentAsString, MessageListResponse::class.java)
        Assertions.assertTrue(owned.messages.isNotEmpty())
        owned.messages.forEach { message ->
            Assertions.assertTrue(message.uuid == message1.uuid || message.uuid == message2.uuid)
            Assertions.assertEquals(subQueue, message.subQueue)
            Assertions.assertEquals(assignedTo, message.assignedTo)
        }
    }

    /**
     * Ensure when [MessageQueueController.getOwned] is called on a restricted sub-queue that no entries are returned
     * unless a valid token is provided.
     */
    @Test
    fun testGetOwned_SomeOwned_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val assignedTo = "my-assigned-to-identifier"
        val subQueue = "testGetOwned_SomeOwned_inHybridMode"
        val message1 = createQueueMessage(assignedTo = assignedTo, subQueue = subQueue)
        val message2 = createQueueMessage(assignedTo = assignedTo, subQueue = subQueue)

        Assertions.assertTrue(multiQueue.add(message1))
        Assertions.assertTrue(multiQueue.add(message2))

        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(authenticator.isRestricted(subQueue))

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNED)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo)
            .param(RestParameters.SUB_QUEUE, subQueue))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        val mvcResult: MvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNED)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo)
            .param(RestParameters.SUB_QUEUE, subQueue))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val owned = gson.fromJson<MessageListResponse>(mvcResult.response.contentAsString, MessageListResponse::class.java)
        Assertions.assertTrue(owned.messages.isNotEmpty())
        owned.messages.forEach { message ->
            Assertions.assertTrue(message.uuid == message1.uuid || message.uuid == message2.uuid)
            Assertions.assertEquals(subQueue, message.subQueue)
            Assertions.assertEquals(assignedTo, message.assignedTo)
        }
    }

    /**
     * Test [MessageQueueController.assignMessage] to ensure that [HttpStatus.NO_CONTENT] is returned when a
     * [QueueMessage] with the provided [UUID] does not exist.
     */
    @Test
    fun testAssignMessage_doesNotExist()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val uuid = UUID.randomUUID().toString()
        val assignedTo = "assigned"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + uuid + MessageQueueController.ENDPOINT_ASSIGN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Test [MessageQueueController.assignMessage] to ensure that the message is assigned correctly and [HttpStatus.OK]
     * is returned when the [QueueMessage] was initially not assigned.
     */
    @Test
    fun testAssignMessage_messageIsAssigned()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assigned"
        val message = createQueueMessage(subQueue = "testAssignMessage_messageIsAssigned")
        Assertions.assertNull(message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_ASSIGN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertEquals(assignedTo, messageResponse.message.assignedTo)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        val assignedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(assignedTo, assignedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, assignedMessage.uuid)
    }

    /**
     * Ensure when [MessageQueueController.assignMessage] is called in [RestrictionMode.HYBRID] mode
     * that the message is not assigned or changed if the sub-queue is restricted and a valid token is not provided.
     */
    @Test
    fun testAssignMessage_messageIsAssigned_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val assignedTo = "assigned"
        val message = createQueueMessage(subQueue = "testAssignMessage_messageIsAssigned_inHybridMode")
        Assertions.assertNull(message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        Assertions.assertTrue(authenticator.addRestrictedEntry(message.subQueue))
        Assertions.assertTrue(authenticator.isRestricted(message.subQueue))

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_ASSIGN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        val token = jwtTokenProvider.createTokenForSubQueue(message.subQueue)
        Assertions.assertTrue(token.isPresent)

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_ASSIGN)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertEquals(assignedTo, messageResponse.message.assignedTo)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        val assignedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(assignedTo, assignedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, assignedMessage.uuid)
    }

    /**
     * Test [MessageQueueController.assignMessage] to ensure that the message is assigned correctly and
     * [HttpStatus.ACCEPTED] is returned when the [QueueMessage] is already assigned by the provided `assignTo`
     * identifier.
     */
    @Test
    fun testAssignMessage_alreadyAssignedToSameID()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assigned"
        val message = createQueueMessage(subQueue = "testAssignMessage_alreadyAssignedToSameID", assignedTo = assignedTo)

        Assertions.assertEquals(assignedTo, message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_ASSIGN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isAccepted)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertEquals(message.assignedTo, messageResponse.message.assignedTo)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        val assignedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(assignedTo, assignedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, assignedMessage.uuid)
    }

    /**
     * Test [MessageQueueController.assignMessage] to ensure that [HttpStatus.CONFLICT] is returned when the
     * [QueueMessage] is already assigned to another identifier.
     */
    @Test
    fun testAssignMessage_alreadyAssignedToOtherID()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val message = createQueueMessage(subQueue = "testAssignMessage_alreadyAssignedToOtherID", assignedTo = assignedTo)

        Assertions.assertEquals(assignedTo, message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        // Check the message is set correctly
        var assignedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(assignedTo, assignedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, assignedMessage.uuid)

        val wrongAssignee = "wrong-assignee"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_ASSIGN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, wrongAssignee))
            .andExpect(MockMvcResultMatchers.status().isConflict)

        // Check the message is still assigned to the correct ID
        assignedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(assignedTo, assignedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, assignedMessage.uuid)
    }

    /**
     * Test [MessageQueueController.getNext] to ensure [HttpStatus.NO_CONTENT] is returned when there are no
     * [QueueMessage]s available for the provided `sub-queue`.
     */
    @Test
    fun testGetNext_noNewMessages()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val subQueue = "testGetNext_noNewMessages"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Assertions.assertTrue(multiQueue.getSubQueue(subQueue).isEmpty())
    }

    /**
     * Test [MessageQueueController.getNext] to ensure that [HttpStatus.NO_CONTENT] is returned if there are no
     * `assigned` [QueueMessage]s available.
     */
    @Test
    fun testGetNext_noNewUnAssignedMessages()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val subQueue = "testGetNext_noNewUnAssignedMessages"
        val message = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo)
        val message2 = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))

        Assertions.assertFalse(multiQueue.getSubQueue(subQueue).isEmpty())

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Test [MessageQueueController.getNext] to ensure that the correct next message is returned if it exists in
     * the [MultiQueue].
     */
    @Test
    fun testGetNext()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val subQueue = "testGetNext"
        val message = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo)
        val message2 = createQueueMessage(subQueue = subQueue)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))

        val storedMessage2 = multiQueue.getSubQueue(subQueue).stream().filter{ m -> m.uuid == message2.uuid }.findFirst().get()
        Assertions.assertNull(storedMessage2.assignedTo)
        Assertions.assertEquals(message2.uuid, storedMessage2.uuid)

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertEquals(assignedTo, messageResponse.message.assignedTo)
        Assertions.assertEquals(message2.uuid, messageResponse.message.uuid)

        val assignedMessage2 = multiQueue.getSubQueue(subQueue).stream().filter{ m -> m.uuid == message2.uuid }.findFirst().get()
        Assertions.assertEquals(assignedTo, assignedMessage2.assignedTo)
        Assertions.assertEquals(message2.uuid, assignedMessage2.uuid)
    }

    /**
     * Ensure that when in [RestrictionMode.HYBRID] mode that the next message for a restricted sub-queue
     * cannot be retrieved without a valid token being provided.
     */
    @Test
    fun testGetNext_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val subQueue = "testGetNext_inHybridMode"
        val message = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo)
        val message2 = createQueueMessage(subQueue = subQueue)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))

        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(authenticator.isRestricted(subQueue))

        val storedMessage2 = multiQueue.getSubQueue(subQueue).stream().filter{ m -> m.uuid == message2.uuid }.findFirst().get()
        Assertions.assertNull(storedMessage2.assignedTo)
        Assertions.assertEquals(message2.uuid, storedMessage2.uuid)

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertEquals(assignedTo, messageResponse.message.assignedTo)
        Assertions.assertEquals(message2.uuid, messageResponse.message.uuid)

        val assignedMessage2 = multiQueue.getSubQueue(subQueue).stream().filter{ m -> m.uuid == message2.uuid }.findFirst().get()
        Assertions.assertEquals(assignedTo, assignedMessage2.assignedTo)
        Assertions.assertEquals(message2.uuid, assignedMessage2.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that [HttpStatus.NO_CONTENT] is returned when a
     * [QueueMessage] with the provided [UUID] does not exist.
     */
    @Test
    fun testReleaseMessage_doesNotExist()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val uuid = UUID.randomUUID().toString()
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + uuid + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that the [QueueMessage] is released if it is currently
     * assigned.
     */
    @Test
    fun testReleaseMessage_messageIsReleased()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val message = createQueueMessage(subQueue = "testReleaseMessage_messageIsReleased", assignedTo = assignedTo)

        Assertions.assertEquals(assignedTo, message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertNull(messageResponse.message.assignedTo)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        val updatedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertNull(updatedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, updatedMessage.uuid)
    }

    /**
     * Ensure that when in [RestrictionMode.HYBRID] mode that a restricted sub-queue cannot have its
     * message released without a valid token being provided.
     */
    @Test
    fun testReleaseMessage_messageIsReleased_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val message = createQueueMessage(subQueue = "testReleaseMessage_messageIsReleased_inHybridMode", assignedTo = assignedTo)

        Assertions.assertEquals(assignedTo, message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        Assertions.assertTrue(authenticator.addRestrictedEntry(message.subQueue))
        Assertions.assertTrue(authenticator.isRestricted(message.subQueue))

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isForbidden)


        val token = jwtTokenProvider.createTokenForSubQueue(message.subQueue)
        Assertions.assertTrue(token.isPresent)

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_RELEASE)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, assignedTo))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertNull(messageResponse.message.assignedTo)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        val updatedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertNull(updatedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, updatedMessage.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that the [QueueMessage] is released if it is currently
     * assigned. Even when the `assignedTo` is not provided.
     */
    @Test
    fun testReleaseMessage_messageIsReleased_withoutAssignedToInQuery()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assigned"
        val message = createQueueMessage(subQueue = "testReleaseMessage_messageIsReleased_withoutAssignedToInQuery", assignedTo = assignedTo)

        Assertions.assertEquals(assignedTo, message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertNull(messageResponse.message.assignedTo)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        val updatedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertNull(updatedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, updatedMessage.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that [HttpStatus.ACCEPTED] is returned if the message
     * is already released and not owned by anyone.
     */
    @Test
    fun testReleaseMessage_alreadyReleased()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testReleaseMessage_alreadyReleased")
        Assertions.assertNull(message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val mvcResult: MvcResult = mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isAccepted)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, MessageResponse::class.java)
        Assertions.assertNull(messageResponse.message.assignedTo)
        Assertions.assertEquals(message.uuid, messageResponse.message.uuid)

        // Ensure the message is updated in the queue too
        val updatedMessage = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertNull(updatedMessage.assignedTo)
        Assertions.assertEquals(message.uuid, updatedMessage.uuid)
    }

    /**
     * Test [MessageQueueController.releaseMessage] to ensure that [HttpStatus.CONFLICT] is returned if `assignedTo`
     * is provided and does not match the [QueueMessage.assignedTo], meaning the user cannot `release` the
     * [QueueMessage] if it's not the current [QueueMessage.assignedTo] of the [QueueMessage].
     */
    @Test
    fun testReleaseMessage_cannotBeReleasedWithMisMatchingID()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assigned"
        val message = createQueueMessage(subQueue = "testReleaseMessage_cannotBeReleasedWithMisMatchingID", assignedTo = assignedTo)

        Assertions.assertEquals(assignedTo, message.assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val wrongAssignedTo = "wrong-assigned"
        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, wrongAssignedTo))
            .andExpect(MockMvcResultMatchers.status().isConflict)

        val assignedEntry = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(assignedTo, assignedEntry.assignedTo)
    }

    /**
     * Test [MessageQueueController.removeMessage] to ensure that [HttpStatus.NO_CONTENT] is returned when a
     * [QueueMessage] with the provided [UUID] does not exist.
     */
    @Test
    fun testRemoveMessage_notFound()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val uuid = UUID.randomUUID().toString()

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)
    }

    /**
     * Test [MessageQueueController.removeMessage] to ensure that a [HttpStatus.OK] is returned when the message is
     * correctly removed.
     */
    @Test
    fun testRemoveMessage_removeExistingEntry()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testRemoveMessage_removed")
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.containsUUID(message.uuid).isPresent)

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)

        Assertions.assertFalse(multiQueue.containsUUID(message.uuid).isPresent)
        Assertions.assertTrue(multiQueue.getSubQueue(message.subQueue).isEmpty())
    }

    /**
     * Ensure when in [RestrictionMode.HYBRID] mode that messages cannot be removed unless a valid
     * token is provided on a restricted sub-queue.
     */
    @Test
    fun testRemoveMessage_removeExistingEntry_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testRemoveMessage_removeExistingEntry_inHybridMode")
        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.containsUUID(message.uuid).isPresent)

        Assertions.assertTrue(authenticator.addRestrictedEntry(message.subQueue))
        Assertions.assertTrue(authenticator.isRestricted(message.subQueue))

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        val token = jwtTokenProvider.createTokenForSubQueue(message.subQueue)
        Assertions.assertTrue(token.isPresent)

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)

        Assertions.assertFalse(multiQueue.containsUUID(message.uuid).isPresent)
        Assertions.assertTrue(multiQueue.getSubQueue(message.subQueue).isEmpty())
    }

    /**
     * Test [MessageQueueController.removeMessage] to ensure that a [HttpStatus.NO_CONTENT] is returned when the
     * matching message does not exist.
     */
    @Test
    fun testRemoveMessage_doesNotExist()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val uuid = UUID.randomUUID().toString()
        Assertions.assertFalse(multiQueue.containsUUID(uuid).isPresent)

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Assertions.assertFalse(multiQueue.containsUUID(uuid).isPresent)
    }

    /**
     * Test [MessageQueueController.removeMessage] to ensure that [HttpStatus.FORBIDDEN] is returned if the message is
     * attempting to be removed while another user is consuming it.
     */
    @Test
    fun testRemoveMessage_assignedToAnotherID()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val message = createQueueMessage(subQueue = "testRemoveMessage_assignedToAnotherID", assignedTo = assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val wrongAssignedTo = "wrong-assignee"
        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, wrongAssignedTo))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        val assignedEntry = multiQueue.peekSubQueue(message.subQueue).get()
        Assertions.assertEquals(assignedTo, assignedEntry.assignedTo)
    }

    /**
     * Test [MessageQueueController.getOwners] with a provided `sub-queue` parameter to ensure the appropriate map is
     * provided in the response and [HttpStatus.OK] is returned.
     */
    @Test
    fun testGetOwners_inSubQueue()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignedTo"
        val assignedTo2 = "assignedTo2"

        val subQueue = "testGetOwners"

        val message = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo)
        val message2 = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo2)
        val message3 = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo2)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))

        val mvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNERS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val owners = gson.fromJson(mvcResult.response.contentAsString, OwnersMapResponse::class.java)
        Assertions.assertNotNull(owners.owners)
        Assertions.assertEquals(2, owners.owners.size)
        Assertions.assertTrue(owners.owners.keys.toList().contains(assignedTo))
        Assertions.assertTrue(owners.owners.keys.toList().contains(assignedTo2))

        val valuesInAssignedTo = owners.owners[assignedTo]
        Assertions.assertNotNull(valuesInAssignedTo)
        Assertions.assertTrue(valuesInAssignedTo!!.contains(subQueue))

        val valuesInAssignedTo2 = owners.owners[assignedTo2]
        Assertions.assertNotNull(valuesInAssignedTo2)
        Assertions.assertTrue(valuesInAssignedTo2!!.contains(subQueue))
    }

    /**
     * Test [MessageQueueController.getOwners] without a provided `sub-queue` parameter to ensure the appropriate map
     * is provided in the response and [HttpStatus.OK] is returned.
     */
    @Test
    fun testGetOwners_notInSubQueue()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignedTo"
        val assignedTo2 = "assignedTo2"

        val subQueue = "testGetOwners"
        val subQueue2 = "testGetOwners2"

        val message = createQueueMessage(subQueue = subQueue, assignedTo = assignedTo)
        val message2 = createQueueMessage(subQueue = subQueue2, assignedTo = assignedTo)
        val message3 = createQueueMessage(subQueue = subQueue2, assignedTo = assignedTo2)

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))

        val mvcResult = mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNERS)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val owners = gson.fromJson(mvcResult.response.contentAsString, OwnersMapResponse::class.java)
        Assertions.assertNotNull(owners.owners)
        Assertions.assertEquals(2, owners.owners.size)
        Assertions.assertTrue(owners.owners.keys.toList().contains(assignedTo))
        Assertions.assertTrue(owners.owners.keys.toList().contains(assignedTo2))

        val valuesInAssignedTo = owners.owners[assignedTo]
        Assertions.assertNotNull(valuesInAssignedTo)
        Assertions.assertTrue(valuesInAssignedTo!!.contains(subQueue))
        Assertions.assertTrue(valuesInAssignedTo.contains(subQueue2))

        val valuesInAssignedTo2 = owners.owners[assignedTo2]
        Assertions.assertNotNull(valuesInAssignedTo2)
        Assertions.assertTrue(valuesInAssignedTo2!!.contains(subQueue2))
    }

    /**
     * Ensure a call to the [MessageQueueController.getHealthCheck] to ensure a [HttpStatus.OK] is returned when the
     * application is running ok.
     */
    @Test
    fun testGetPerformHealthCheck()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_HEALTH_CHECK)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    /**
     * Ensure that the [CorrelationIdFilter] will generate a random Correlation ID when one is not provided and that
     * it is returned in the [MessageResponse].
     */
    @Test
    fun testCorrelationId_randomIdOnSuccess()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testCorrelationId_providedId")

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val correlationIdHeader = mvcResult.response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
        Assertions.assertNotNull(correlationIdHeader)
    }

    /**
     * Ensure that the [CorrelationIdFilter] will use the same correlationID that is provided is used and returned in
     * the [MessageResponse].
     */
    @Test
    fun testCorrelationId_providedId()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = createQueueMessage(subQueue = "testCorrelationId_providedId")
        val correlationId = "my-correlation-id-123456"

        val mvcResult: MvcResult = mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
            .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val correlationIdHeader = mvcResult.response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
        Assertions.assertNotNull(correlationIdHeader)
        Assertions.assertEquals(correlationId, correlationIdHeader)
    }

    /**
     * Ensure that the [CorrelationIdFilter] will generate a random Correlation ID is generated on error and returned
     * in the response.
     */
    @Test
    fun testCorrelationId_randomIdOnError()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val assignedTo = "assignee"
        val message = createQueueMessage(subQueue = "testCorrelationId_randomIdOnError", assignedTo = assignedTo)
        Assertions.assertTrue(multiQueue.add(message))

        val wrongAssignedTo = "wrong-assignee"
        val mvcResult: MvcResult = mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.ASSIGNED_TO, wrongAssignedTo))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andReturn()

        val messageResponse = gson.fromJson(mvcResult.response.contentAsString, Map::class.java)
        Assertions.assertNotNull(messageResponse)
        Assertions.assertTrue(messageResponse.containsKey(CorrelationIdFilter.CORRELATION_ID))
        val correlationId = messageResponse[CorrelationIdFilter.CORRELATION_ID]
        Assertions.assertTrue(correlationId is String)
        Assertions.assertEquals(correlationId, UUID.fromString(correlationId as String).toString())

        val correlationIdHeader = mvcResult.response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)
        Assertions.assertNotNull(correlationIdHeader)
        Assertions.assertEquals(correlationId, correlationIdHeader)
    }

    /**
     * Ensure that [MessageQueueController.deleteKeys] will only delete keys by the specified
     * [RestParameters.SUB_QUEUE] when it is provided and that other sub-queues are not cleared.
     */
    @Test
    fun testDeleteKeys_singleKey()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val subQueue1 = "testDeleteKeys_singleKey1"
        var messages = listOf(createQueueMessage(subQueue1), createQueueMessage(subQueue1), createQueueMessage(subQueue1))
        messages.forEach { message -> Assertions.assertTrue(multiQueue.add(message)) }

        Assertions.assertEquals(3, multiQueue.size)

        val subQueue2 = "testDeleteKeys_singleKey2"
        messages = listOf(createQueueMessage(subQueue2), createQueueMessage(subQueue2))
        messages.forEach { message -> Assertions.assertTrue(multiQueue.add(message)) }

        Assertions.assertEquals(5, multiQueue.size)

        Assertions.assertTrue(multiQueue.keys().contains(subQueue1))
        Assertions.assertTrue(multiQueue.keys().contains(subQueue2))

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue1))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Assertions.assertEquals(2, multiQueue.size)
        Assertions.assertFalse(multiQueue.keys().contains(subQueue1))
        Assertions.assertTrue(multiQueue.keys().contains(subQueue2))
    }

    /**
     * Ensure that when in [RestrictionMode.HYBRID] mode any restricted sub-queues cannot be deleted
     * unless a valid token is provided.
     */
    @Test
    fun testDeleteKeys_singleKey_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val subQueue1 = "testDeleteKeys_singleKey_inHybridMode1"
        var messages = listOf(createQueueMessage(subQueue1), createQueueMessage(subQueue1), createQueueMessage(subQueue1))
        messages.forEach { message -> Assertions.assertTrue(multiQueue.add(message)) }

        Assertions.assertEquals(3, multiQueue.size)

        val subQueue2 = "testDeleteKeys_singleKey_inHybridMode2"
        messages = listOf(createQueueMessage(subQueue2), createQueueMessage(subQueue2))
        messages.forEach { message -> Assertions.assertTrue(multiQueue.add(message)) }

        Assertions.assertEquals(5, multiQueue.size)

        Assertions.assertTrue(multiQueue.keys().contains(subQueue1))
        Assertions.assertTrue(multiQueue.keys().contains(subQueue2))

        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueue2))
        Assertions.assertTrue(authenticator.isRestricted(subQueue2))

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue1))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Assertions.assertEquals(2, multiQueue.size)
        Assertions.assertFalse(multiQueue.keys().contains(subQueue1))
        Assertions.assertTrue(multiQueue.keys().contains(subQueue2))

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue2))
            .andExpect(MockMvcResultMatchers.status().isForbidden)

        val token = jwtTokenProvider.createTokenForSubQueue(subQueue2)
        Assertions.assertTrue(token.isPresent)

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param(RestParameters.SUB_QUEUE, subQueue2))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Assertions.assertEquals(0, multiQueue.size)
        Assertions.assertFalse(multiQueue.keys().contains(subQueue1))
        Assertions.assertFalse(multiQueue.keys().contains(subQueue2))
    }

    /**
     * Ensure that [MessageQueueController.deleteKeys] will only delete all keys/queues when the provided
     * [RestParameters.SUB_QUEUE] is `null`.
     */
    @Test
    fun testDeleteKeys_allKeys()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val (messages, subQueues) = initialiseMapWithEntries()
        Assertions.assertEquals(messages.size, multiQueue.size)
        subQueues.forEach { subQueue -> Assertions.assertTrue(multiQueue.keys().contains(subQueue)) }

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Assertions.assertTrue(multiQueue.isEmpty())
        subQueues.forEach { subQueue -> Assertions.assertFalse(multiQueue.keys().contains(subQueue)) }
    }

    /**
     * Ensure that when in [RestrictionMode.HYBRID] mode any restricted sub-queues cannot be deleted
     * unless a valid token is provided.
     */
    @Test
    fun testDeleteKeys_allKeys_inHybridMode()
    {
        Mockito.doReturn(RestrictionMode.HYBRID).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.HYBRID, authenticator.getRestrictionMode())

        val (messages, subQueues) = initialiseMapWithEntries()
        Assertions.assertEquals(messages.size, multiQueue.size)
        subQueues.forEach { subQueue -> Assertions.assertTrue(multiQueue.keys().contains(subQueue)) }

        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueues[0]))
        Assertions.assertTrue(authenticator.isRestricted(subQueues[0]))

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isPartialContent)

        Assertions.assertEquals(1, multiQueue.size)
        Assertions.assertTrue(multiQueue.keys().contains(subQueues[0]))
        subQueues.subList(1, subQueues.size - 1).forEach { subQueue -> Assertions.assertFalse(multiQueue.keys().contains(subQueue)) }

        val token = jwtTokenProvider.createTokenForSubQueue(subQueues[0])
        Assertions.assertTrue(token.isPresent)

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_KEYS)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Assertions.assertTrue(multiQueue.isEmpty())
        subQueues.forEach { subQueue -> Assertions.assertFalse(multiQueue.keys().contains(subQueue)) }
    }

    /**
     * `Mock Test`.
     *
     * Perform a health check call to the [MessageQueueController.getHealthCheck] to ensure a
     * [HttpStatus.INTERNAL_SERVER_ERROR] is returned when the health check fails.
     */
    @Test
    fun testGetPerformHealthCheck_failureResponse()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        Mockito.doThrow(RuntimeException("Failed to perform health check.")).`when`(multiQueue).performHealthCheckInternal()

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_HEALTH_CHECK)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
            .andReturn()
    }

    /**
     * `Mock Test`.
     *
     * Test [MessageQueueController.createMessage] to ensure that an internal server error is returned when [MultiQueue.add] returns `false`.
     */
    @Test
    fun testCreateMessage_addFails()
    {
        Assertions.assertEquals(RestrictionMode.NONE, authenticator.getRestrictionMode())

        val message = QueueMessage("payload", "testCreateMessage_addFails")

        Mockito.doReturn(false).`when`(multiQueue).add(message)

        mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(gson.toJson(message)))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    /**
     * Ensure that when [RestrictionMode] is set to [RestrictionMode.RESTRICTED] that any
     * of the endpoints failing the [JwtAuthenticationFilter.canSkipTokenVerification] will be inaccessible.
     */
    @Test
    fun testRestrictedModeMakesAllEndpointsInaccessibleWithoutAToken()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, authenticator.getRestrictionMode())

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        mockMvc.perform(delete(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        val message = QueueMessage("", "testRestrictedModeMakesAllEndpointsInaccessible")
        mockMvc.perform(post(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY)
            .content(gson.toJson(message))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + MessageQueueController.ENDPOINT_ENTRY + "/" + UUID.randomUUID().toString() + MessageQueueController.ENDPOINT_RELEASE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + UUID.randomUUID().toString() + MessageQueueController.ENDPOINT_ASSIGN)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        mockMvc.perform(put(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_NEXT + "?" + RestParameters.SUB_QUEUE +"=someType&" + RestParameters.ASSIGNED_TO + "=me")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ALL)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_OWNED + "?" + RestParameters.SUB_QUEUE +"=someType&" + RestParameters.ASSIGNED_TO + "=me")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    /**
     * Ensure that the [JwtAuthenticationFilter] will respond with [HttpStatus.UNAUTHORIZED] when no token is provided,
     * and it is in [RestrictionMode.RESTRICTED] mode.
     */
    @Test
    fun testGetEntry_inRestrictedMode()
    {
        Mockito.doReturn(RestrictionMode.RESTRICTED).`when`(authenticator).getRestrictionMode()
        Assertions.assertEquals(RestrictionMode.RESTRICTED, authenticator.getRestrictionMode())

        val subQueue = "testGetEntry_inRestrictedMode"
        val message = createQueueMessage(subQueue)

        Assertions.assertTrue(multiQueue.add(message))

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        val token = jwtTokenProvider.createTokenForSubQueue(subQueue)
        Assertions.assertTrue(token.isPresent)

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        Assertions.assertTrue(authenticator.addRestrictedEntry(subQueue))
        Assertions.assertTrue(authenticator.isRestricted(subQueue))

        mockMvc.perform(get(MessageQueueController.MESSAGE_QUEUE_BASE_PATH + "/" + MessageQueueController.ENDPOINT_ENTRY + "/" + message.uuid)
            .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "${JwtAuthenticationFilter.BEARER_HEADER_VALUE}${token.get()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    /**
     * A helper method which creates `4` [QueueMessage] objects and inserts them into the [MultiQueue].
     *
     * @return a [Pair] containing the [List] of [QueueMessage] and their related matching [List] of [String] `sub-queue` IDs in order.
     */
    private fun initialiseMapWithEntries(): Pair<List<QueueMessage>, List<String>>
    {
        val subQueues = listOf("type1", "type2", "type3", "type4")
        val message = createQueueMessage(subQueue = subQueues[0])
        val message2 = createQueueMessage(subQueue = subQueues[1])
        val message3 = createQueueMessage(subQueue = subQueues[2], assignedTo = "assignee")
        val message4 = createQueueMessage(subQueue = subQueues[3])

        Assertions.assertTrue(multiQueue.add(message))
        Assertions.assertTrue(multiQueue.add(message2))
        Assertions.assertTrue(multiQueue.add(message3))
        Assertions.assertTrue(multiQueue.add(message4))

        return Pair(listOf(message, message2, message3, message4), subQueues)
    }

    /**
     * A helper method to create a [QueueMessage] that can be easily re-used between each test.
     *
     * @param subQueue the `subQueue` to set in to the created [QueueMessage]
     * @param assignedTo the [QueueMessage.assignedTo] value to set
     * @return a [QueueMessage] initialised with multiple parameters
     */
    private fun createQueueMessage(subQueue: String, assignedTo: String? = null): QueueMessage
    {
        val uuid = UUID.randomUUID().toString()
        val payload = Payload("test", 12, true, PayloadEnum.C)
        val message = QueueMessage(payload = payload, subQueue = subQueue)
        message.uuid = UUID.fromString(uuid).toString()

        message.assignedTo = assignedTo
        return message
    }
}
