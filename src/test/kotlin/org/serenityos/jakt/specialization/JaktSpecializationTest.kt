package org.serenityos.jakt.specialization

import com.intellij.psi.PsiNameIdentifierOwner
import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest
import org.serenityos.jakt.psi.ancestors
import org.serenityos.jakt.render.renderElement
import org.serenityos.jakt.utils.padWithNulls

abstract class JaktSpecializationTest : JaktBaseTest() {
    fun doTest(@Language("Jakt") text: String, vararg renderedTypes: String) {
        setupFor(text)

        val taggedElements = extractTaggedElements().map { (key, value) ->
            check(key.startsWith("T")) {
                "Invalid specialization tag $key"
            }

            check(value.size == 1) {
                "Type tag resolved to ${value.size} elements (should not point to a declaration)"
            }

            val num = key.substring(1).ifBlank { "1" }.toIntOrNull()
            check(num != null) {
                "Invalid specialization tag $key"
            }

            check(num >= 1) {
                "Specialization tag number must be absent or greater than 0"
            }

            val element = value.single().let {
                it.ancestors().find { a -> (a as? PsiNameIdentifierOwner)?.nameIdentifier == it } ?: it
            }

            num - 1 to renderElement(element, asHtml = false)
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
