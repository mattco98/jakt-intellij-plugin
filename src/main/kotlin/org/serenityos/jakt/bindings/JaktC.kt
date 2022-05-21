package org.serenityos.jakt.bindings

import com.sun.jna.Library
import com.sun.jna.Native
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.serenityos.jakt.bindings.types.LexResult
import org.serenityos.jakt.bindings.types.ParseResult
import java.io.File

interface JaktC : Library {
    fun lex(string: String): String
    fun parse(string: String): String

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalSerializationApi::class)
    companion object {
        private val INSTANCE = Native.load("/libjakt.so", JaktC::class.java) as JaktC

        fun lex(file: File): LexResult {
            require(file.exists())
            return lex(file.readText())
        }

        fun lex(string: String): LexResult {
            return Json.decodeFromString(INSTANCE.lex(string))
        }

        fun parse(file: File): ParseResult {
            require(file.exists())
            return parse(file.readText())
        }

        fun parse(string: String): ParseResult {
            return Json.decodeFromString(INSTANCE.parse(string))
        }
    }
}
