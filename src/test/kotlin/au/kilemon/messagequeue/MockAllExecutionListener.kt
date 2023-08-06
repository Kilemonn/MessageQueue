package au.kilemon.messagequeue

import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.Ordered
import org.springframework.stereotype.Service
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.util.ReflectionUtils.makeAccessible
import org.springframework.util.ReflectionUtils.setField
import java.lang.reflect.Constructor
import java.util.*
import javax.annotation.Resource
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.KClass

/**
 * A special [TestExecutionListener] that will [Mockito.mock] ALL injected beans in the context.
 * Except the beans marked as [NotMocked], or if they are in the [NotMocked.spyClasses] list they will be created as [Mockito.spy] objects.
 *
 * @author github.com/Kilemonn
 */
class MockAllExecutionListener : TestExecutionListener, Ordered
{
    private val injectableAnnotation: List<Class<out Annotation>> = injectableAnnotations()

    private val spyKClasses: HashSet<KClass<*>> = HashSet()

    private val initialisedMocks = HashMap<Class<*>, Any>()

    private lateinit var testContext: TestContext

    override fun getOrder(): Int
    {
        return 10000
    }

    /**
     * Returns the held [testContext], this method is here to be used for mocking.
     */
    private fun getTestContext(): TestContext
    {
        return testContext
    }

    /**
     * Set the stored reference to the [TestContext]
     *
     * @param testContext the new [TestContext] to set
     */
    private fun setTestContext(testContext: TestContext)
    {
        this.testContext = testContext
    }

    /**
     * The entry point for the test instance initialisation.
     * This will iterate over all fields in the test class and inject any mocks required. This will inject mocks into
     * parent class members.
     *
     * @param testContext the [TestContext]
     */
    override fun prepareTestInstance(testContext: TestContext)
    {
        setTestContext(testContext)
        initialisedMocks[testContext.testClass] = testContext.testInstance
        var clazz = testContext.testClass
        do
        {
            mockAnnotationFields(clazz)
            clazz = clazz.superclass
        } while (clazz != Any::class.java)
    }

    /**
     * This method will create a [Mockito.mock] of any field marked with an [injectableAnnotations].
     * Or, if the field is contained in [spyKClasses] it will be created as a [Mockito.spy].
     * The [NotMocked] class will be instantiated using a first constructor found.
     *
     * @param clazz the current class that we should process
     */
    private fun mockAnnotationFields(clazz: Class<*>)
    {
        for (field in clazz.declaredFields)
        {
            makeAccessible(field)

            val notMocked: NotMocked? = field.getAnnotation(NotMocked::class.java)
            if (notMocked != null)
            {
                spyKClasses.addAll(notMocked.spyClasses.asList())

                setField(field, getTestContext().testInstance, createActualOrSpy(field.type.kotlin))
                mockAnnotationFields(field.type)
            }

            val hasAnyInjectionAnnotations = injectableAnnotation.stream().map { annotation -> field.getAnnotation(annotation) }.toList().filterNotNull().isNotEmpty()
            if (hasAnyInjectionAnnotations)
            {
                setField(field, createOrGetInstance(clazz), createMockOrSpy(field.type, spyKClasses.contains(field.type.kotlin)))
                mockAnnotationFields(field.type)
            }
        }
    }

    /**
     * Create an actual object of [T] or a [Mockito.spy] depending on whether the provided [KClass] exists in the [spyKClasses].
     * If we need to create an actual object we will delegate the creation to the [findZeroArgConstructor] to find the correct constructor.
     *
     * @param kClass the incoming class that we should create an instance or [Mockito.spy] of
     * @return the constructed [Mockito.spy] OR instance of [T]
     */
    private fun <T : Any> createActualOrSpy(kClass: KClass<T>): T
    {
        val shouldSpyNotMocked = spyKClasses.contains(kClass)
        return if (shouldSpyNotMocked)
        {
            createOrGetInstance(kClass.java, true)
        }
        else
        {
            findZeroArgConstructor(kClass.java).newInstance()
        }
    }

    /**
     * Find the [clazz]'s zero arg constructor.
     *
     * @param clazz the [Class] to find the zero arg constructor of
     * @return the zero argument constructor for the provided [Class]
     * @throws IllegalArgumentException if there is no zero argument constructor defined for this [clazz]
     */
    private fun <T> findZeroArgConstructor(clazz: Class<T>): Constructor<T>
    {
        val constructors: Array<out Constructor<*>> = clazz.declaredConstructors
        val defaultConstructor = Arrays.stream(constructors).filter { constructor -> constructor.parameterCount == 0 }.findFirst()

        if (defaultConstructor.isEmpty)
        {
            throw IllegalArgumentException("Unable to find default zero argument constructor for class [" + clazz.name + "] for usage with [" + NotMocked::class.qualifiedName + "].")
        }

        return defaultConstructor.get() as Constructor<T>
    }

    /**
     * Create the provided [Class] of type [T] instance as either a [Mockito.mock] or [Mockito.spy] based on the provided [createSpy].
     *
     * @param clazz the class to create a [Mockito.mock] or [Mockito.spy] of
     * @param createSpy *true* to create a [Mockito.spy], otherwise will create [Mockito.mock]
     * @return the created [Mockito.mock] or [Mockito.spy] of type [T]
     */
    private fun <T> createMockOrSpy(clazz: Class<T>, createSpy: Boolean = false): T
    {
        return if (createSpy)
        {
            Mockito.spy(clazz)
        }
        else
        {
            Mockito.mock(clazz)
        }
    }

    /**
     * Create the provided [Class] T instance as either a [Mockito.mock]
     */
    private fun <T> createOrGetInstance(clazz: Class<T>, createSpy: Boolean = false): T
    {
        return if (initialisedMocks.contains(clazz))
        {
            initialisedMocks[clazz] as T
        }
        else
        {
            createMockOrSpy(clazz, createSpy)
        }
    }

    /**
     * Return a list of Injectable [Annotation]s. If any member field has any of these, we will attempt to mock it.
     *
     * @return [List] of [Annotation] that should be injected with a mock
     */
    private fun injectableAnnotations(): List<Class<out Annotation>>
    {
        return listOf(Autowired::class.java, Resource::class.java, Service::class.java)
    }
}
