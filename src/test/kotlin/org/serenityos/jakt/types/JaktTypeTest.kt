package org.serenityos.jakt.types

import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest
import org.serenityos.jakt.psi.JaktTypeable
import org.serenityos.jakt.psi.ancestors
import org.serenityos.jakt.render.renderType
import org.serenityos.jakt.utils.padWithNulls

abstract class JaktTypeTest : JaktBaseTest() {
    fun doTest(@Language("Jakt") text: String, vararg renderedTypes: String) {
        setupFor(text)

        val taggedElements = extractTaggedElements().map { (key, value) ->
            check(key.startsWith("T")) {
                "Invalid type tag $key"
            }

            check(value.size == 1) {
                "Type tag resolved to ${value.size} elements (should not point to a declaration)"
            }

            val num = key.substring(1).ifBlank { "1" }.toIntOrNull()
            check(num != null) {
                "Invalid type tag $key"
            }

            check(num >= 1) {
                "Type tag number must be absent or greater than 0"
            }

            val type = value.single().let {
                it.ancestors().filterIsInstance<JaktTypeable>().firstOrNull()?.jaktType
                    ?: error("Type tag does not point to a typeable element")
            }

            num - 1 to renderType(type)
        }

        check(taggedElements.map { it.first }.toSet().size == taggedElements.size) {
            "Duplicate type tag"
        }

        check(taggedElements.maxOf { it.first } == taggedElements.lastIndex) {
            "Type tags skip number (must be 1..N)"
        }

        check(taggedElements.size == renderedTypes.size) {
            "Number of types provided (${renderedTypes.size}) does not match number of elements tagged (${taggedElements.size})"
        }

        val elements = let {
            val list = mutableListOf<String?>()
            list.padWithNulls(taggedElements.size)
            taggedElements.forEach { (key, value) ->
                list[key] = value
            }
            list.requireNoNulls()
        }.zip(renderedTypes)

        val nonMatching = elements.find { it.first != it.second }
        if (nonMatching != null)
            error("Type ${nonMatching.first} does not match type ${nonMatching.second}")
    }
}
