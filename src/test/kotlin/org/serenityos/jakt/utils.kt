package org.serenityos.jakt

fun <T : Any?> MutableCollection<T>.pad(minSize: Int, default: () -> T) {
    repeat(minSize - size) { add(default()) }
}

fun <T> MutableCollection<T?>.padWithNulls(minSize: Int) = pad(minSize) { null }
