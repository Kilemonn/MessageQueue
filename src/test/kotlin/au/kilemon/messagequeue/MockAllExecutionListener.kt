package au.kilemon.messagequeue

import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.Ordered
import org.springframework.stereotype.Service
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.util.ReflectionUtils.makeAccessible
import org.springframework.util.ReflectionUtils.setField
import javax.annotation.Resource
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

    private val spyClasses: HashSet<KClass<*>> = HashSet()

    private val initialisedMocks = HashMap<Class<*>, Any>()

    private lateinit var testContext: TestContext

    override fun getOrder(): Int
    {
        return 10000
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
        this.testContext = testContext
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
     * Or, if the field is contained in [spyClasses] it will be created as a [Mockito.spy].
     * The [NotMocked] class will be instanciated using a first constructor found.
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
                spyClasses.addAll(notMocked.spyClasses.asList())

                val shouldSpyNotMocked = spyClasses.contains(field.type.kotlin)
                setField(field, testContext.testInstance, if (shouldSpyNotMocked) { createOrGetInstance(field.type, true)} else { field.type.getDeclaredConstructor().newInstance() })
                mockAnnotationFields(field.type)
            }

            val hasAnyInjectionAnnotations = injectableAnnotation.stream().map { annotation -> field.getAnnotation(annotation) }.toList().filterNotNull().isNotEmpty()
            if (hasAnyInjectionAnnotations)
            {
                setField(field, createOrGetInstance(clazz), createMockOrSpy(field.type, spyClasses.contains(field.type.kotlin)))
                mockAnnotationFields(field.type)
            }
        }
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
        return listOf(Autowired::class.java, Resource::class.java, Service::class.java);
    }
}
