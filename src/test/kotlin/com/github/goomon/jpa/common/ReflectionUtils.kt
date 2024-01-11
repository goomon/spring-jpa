package com.github.goomon.jpa.common

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object ReflectionUtils {
    @Suppress("UNCHECKED_CAST")
    fun <T> invokeMethod(target: Any, methodName: String, vararg parameters: Any): T {
        return try {
            val parameterClasses = parameters.map { it.javaClass }.toTypedArray()
            val method = getMethod(target::class.java, methodName, *parameterClasses)
            method.trySetAccessible()
            method.invoke(target, *parameters) as T
        } catch (e: InvocationTargetException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        }
    }

    private fun getMethod(targetClass: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        return try {
            targetClass.getDeclaredMethod(methodName, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            try {
                targetClass.getMethod(methodName, *parameterTypes)
            } catch (e: NoSuchMethodException) {
                // skipped
            }
            if (targetClass.superclass != Any::class.java) {
                getMethod(targetClass.superclass, methodName, *parameterTypes)
            } else {
                throw e
            }
        }
    }
}
