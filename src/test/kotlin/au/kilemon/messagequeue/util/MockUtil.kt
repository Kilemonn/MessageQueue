package au.kilemon.messagequeue.util

import org.mockito.Mockito

/**
 * MockUtil class to help with any specific mocking workarounds in tests.
 *
 * @author github.com/Kilemonn
 */
class MockUtil
{
    companion object
    {
        /**
         * Helper method to return [Mockito.any] as a non-null type [T].
         * This is required for Kotlin to mock non-nullable parameters.
         */
        fun <T> any(): T
        {
            Mockito.any<T>()
            return uninitialized()
        }

        private fun <T> uninitialized(): T = null as T
    }
}
