package org.serenityos.jakt.utils

import com.intellij.openapi.application.ReadAction

fun runInReadAction(block: () -> Unit) = ReadAction.run<Throwable>(block)

fun unreachable(): Nothing = error("unreachable")

fun <T> Array<T>.indicesOfAll(predicate: (T) -> Boolean): Set<Int> {
    val indices = mutableSetOf<Int>()
    for ((index, value) in withIndex()) {
        if (predicate(value))
            indices.add(index)
    }
    return indices
}

inline fun <T : Any?> MutableCollection<T>.pad(minSize: Int, crossinline default: () -> T) {
    repeat(minSize - size) { add(default()) }
}

inline fun <T : Any?> MutableCollection<T>.padded(minSize: Int, crossinline default: () -> T) =
    toMutableList().also { it.pad(minSize, default) }

fun <T> MutableCollection<T?>.padWithNulls(minSize: Int) = pad(minSize) { null }
