package org.serenityos.jakt.comptime

import com.intellij.openapi.util.TextRange

class InterpreterException(message: String, val range: TextRange) : Exception(message)
