package org.serenityos.jakt.syntax

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key

object JaktParserUtil : GeneratedParserUtilBase() {
    private val MODES_KEY = Key.create<MutableMap<String, Int>>("MODES_KEY")

    private var PsiBuilder.parsingModes: MutableMap<String, Int>
        get() = getUserData(MODES_KEY) ?: mutableMapOf<String, Int>().also {
            putUserData(MODES_KEY, it)
        }
        set(value) = putUserData(MODES_KEY, value)

    @JvmStatic
    fun withOn(builder: PsiBuilder, level: Int, parser: Parser, vararg modes: String): Boolean {
        val parsingModes = builder.parsingModes
        val parsingModesOld = parsingModes.toMutableMap()
        modes.forEach {
            val value = builder.parsingModes[it] ?: return@forEach
            builder.parsingModes[it] = value + 1
        }

        return parser.parse(builder, level).also {
            builder.parsingModes = parsingModesOld
        }
    }

    @JvmStatic
    fun withOff(builder: PsiBuilder, level: Int, parser: Parser, vararg modes: String): Boolean {
        val parsingModes = builder.parsingModes
        val parsingModesOld = parsingModes.toMutableMap()

        modes.forEach { builder.parsingModes[it] = 0 }

        return parser.parse(builder, level).also {
            builder.parsingModes = parsingModesOld
        }
    }
}
