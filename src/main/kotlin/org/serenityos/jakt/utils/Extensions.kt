package org.serenityos.jakt.utils

import com.intellij.openapi.application.ReadAction

fun runInReadAction(block: () -> Unit) = ReadAction.run<Throwable>(block)

fun unreachable(): Nothing = error("unreachable")
