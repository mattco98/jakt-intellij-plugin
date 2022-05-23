package org.serenityos.jakt.bindings

import com.sun.jna.Library
import com.sun.jna.Native
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File


interface JaktC : Library {
    fun typecheck(content: String): String

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalSerializationApi::class)
    companion object {
        private val INSTANCE = Native.load("/libjakt.so", JaktC::class.java) as JaktC

        fun typecheck(file: File): TypecheckResult {
            require(file.exists())
            return typecheck(file.readText())
        }

        fun typecheck(content: String): TypecheckResult {
            return Json.decodeFromString(INSTANCE.typecheck(content))
        }
    }
}

fun main() {
    val result = JaktC.typecheck("""
        function main() {
            let a = b
            return 2
        }
    """.trimIndent())
}
