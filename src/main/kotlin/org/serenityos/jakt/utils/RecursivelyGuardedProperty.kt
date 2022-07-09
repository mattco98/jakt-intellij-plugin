package org.serenityos.jakt.utils

import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.caching.typeCache
import kotlin.reflect.KProperty

/**
 * A delegation function which allows a field to recursively access itself
 * in its initializer.
 *
 * The field initialization happens in two stages:
 *   - The first stage is a producer stage, where an uninitialized object is produced
 *   - The second stage is the initialization of the produced object
 *
 * If, while in the second stage, the field is re-accessed, the object from the first
 * stage is returned without an attempt to enter the second stage, preventing a
 * StackOverflowException.
 *
 * As a practical example, take the following Jakt code:
 *
 *     struct Foo {
 *         function bar() -> Foo {
 *             // ...
 *         }
 *     }
 *
 * If we naively try to resolve the type of Foo without any recursion guards,
 * the StructDecl will eventually try to resolve the FunctionDecl, which will resolve
 * the return type, which will attempt to again resolve the type of Foo, leading to a
 * stack overflow.
 *
 * Instead, we use this delegator and produce an empty struct type in the producer stage.
 * We only attempt to do resolution in the initialization stage.
 */
fun <T : Any> recursivelyGuarded(builder: RecursivelyGuardedPropertyBuilder<T>.() -> Unit): RecursivelyGuardedProperty<T> {
    val propertyBuilder = RecursivelyGuardedPropertyBuilder<T>()
    propertyBuilder.builder()
    return RecursivelyGuardedProperty(propertyBuilder.producerBlock, propertyBuilder.initializerBlock)
}

class RecursivelyGuardedProperty<T : Any>(
    private val producer: () -> T,
    private val initializer: (T) -> Unit,
) {
    private var value = producer()
    private var isInitializing = false

    operator fun getValue(thisRef: PsiElement, property: KProperty<*>): T {
        if (isInitializing)
            return value

        return thisRef.typeCache().resolveWithCaching(thisRef) {
            isInitializing = true
            value = producer()
            initializer(value)
            isInitializing = false

            value
        }
    }
}

class RecursivelyGuardedPropertyBuilder<T> {
    lateinit var producerBlock: () -> T
    lateinit var initializerBlock: (T) -> Unit

    fun producer(block: () -> T) {
        producerBlock = block
    }

    fun initializer(block: (T) -> Unit) {
        initializerBlock = block
    }
}
