package au.kilemon.messagequeue

import kotlin.reflect.KClass

/**
 * An annotation needed for [MockAllExecutionListener].
 * This is used to specify which is the main bean that is going to be tested, and also specify a list of [KClass]
 * that should be created as [Spy] objects instead of [Mock]s for this test.
 *
 * @author github.com/Kilemonn
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NotMocked(val spyClasses: Array<KClass<*>>)
