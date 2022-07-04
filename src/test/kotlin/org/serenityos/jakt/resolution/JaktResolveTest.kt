package org.serenityos.jakt.resolution

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.intellij.lang.annotations.Language
import org.serenityos.jakt.JaktBaseTest
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.declaration.JaktDeclaration
import org.serenityos.jakt.utils.pad
import org.serenityos.jakt.utils.padWithNulls

abstract class JaktResolveTest : JaktBaseTest() {
    data class ResolvedElements(
        val unresolvableElements: List<PsiElement>,
        val references: List<Set<PsiElement>>,
        val declarations: List<PsiElement>,
        val declarationsWithoutRefs: List<PsiElement>,
    )

    private fun getResolvedElements(): ResolvedElements {
        val taggedElements = extractTaggedElements()

        val unresolvableElements = mutableListOf<PsiElement>()
        val references = mutableListOf<MutableSet<PsiElement>>()
        val declarations = mutableListOf<PsiElement?>()
        val declarationsWithoutRefs = mutableListOf<PsiElement>()

        for ((name, elements) in taggedElements) {
            when {
                name == "U" -> unresolvableElements.addAll(elements.map(::getAncestorOfTypeChecked))
                name == "N" -> declarationsWithoutRefs.addAll(elements.map(::getAncestorOfTypeChecked))
                name.startsWith("D") -> {
                    val index = name.substring(1).ifEmpty { "1" }.toInt() - 1
                    declarations.padWithNulls(index + 1)

                    check(declarations[index] == null)
                    check(elements.size == 1) {
                        "Multiple declarations with tag \"$name\""
                    }

                    declarations[index] = getAncestorOfTypeChecked<JaktDeclaration>(elements[0])
                }
                name.startsWith("R") -> {
                    val index = name.substring(1).ifEmpty { "1" }.toInt() - 1
                    references.pad(index + 1) { mutableSetOf() }

                    check(references[index].isEmpty())
                    for (element in elements)
                        references[index].add(getAncestorOfTypeChecked(element))
                }
                else -> error("Unknown tag: \"$name\"")
            }
        }

        check(references.size == declarations.size) {
            "Number of uniquely tagged references (${references.size}) does not match number of " +
                "uniquely tagged declarations (${declarations.size})"
        }

        return ResolvedElements(
            unresolvableElements,
            references,
            declarations.requireNoNulls(),
            declarationsWithoutRefs,
        )
    }

    private inline fun <reified T> getAncestorOfTypeChecked(element: PsiElement): T {
        val ancestor = element.ancestorOfType<T>()!!
        check(ancestor is PsiNameIdentifierOwner)
        check(ancestor.nameIdentifier == element)
        return ancestor
    }

    fun doTest(@Language("Jakt") text: String) {
        setupFor(text)

        check(!PsiTreeUtil.hasErrorElements(myFixture.file)) {
            "Failed to parse test"
        }

        ReadAction.run<Throwable> {
            val taggedElements = getResolvedElements()

            for (unresolvableElement in taggedElements.unresolvableElements) {
                val decl = unresolvableElement.reference!!.resolve()
                if (decl != null) {
                    resolveError(
                        "Annotated unresolvable reference resolved to a definition",
                        "Reference" to unresolvableElement,
                        "Definition" to decl,
                    )
                }
            }

            for (decl in taggedElements.declarationsWithoutRefs) {
                val references = ReferencesSearch.search(decl, LocalSearchScope(myFixture.file)).toList()
                if (references.isNotEmpty()) {
                    resolveError(
                        "Annotated non-referenced declaration has references",
                        "Declaration" to decl,
                        "First reference" to references[0]!! as PsiElement,
                    )
                }
            }

            // Explicitly check that all declarations are referenced
            val declarationsReferences = Array(taggedElements.declarations.size) { false }

            for ((index, refSet) in taggedElements.references.withIndex()) {
                val expectedDecl = taggedElements.declarations[index]
                for (ref in refSet) {
                    val actualDecl = ref.reference!!.resolve()
                        ?: resolveError(
                            "Annotated reference resolves to null",
                            "Reference" to ref,
                            "Expected declaration" to expectedDecl,
                        )

                    if (PsiUtilCore.compareElementsByPosition(expectedDecl, actualDecl) != 0) {
                        resolveError(
                            "Annotated reference refers to the wrong declaration",
                            "Reference" to ref,
                            "Expected declaration" to expectedDecl,
                            "Actual declaration" to actualDecl,
                        )
                    }

                    declarationsReferences[index] = true
                }
            }

            for ((index, isReferenced) in declarationsReferences.withIndex()) {
                check(isReferenced) {
                    "Declaration #$index is never referenced"
                }
            }
        }
    }

    private fun resolveError(message: String, vararg errorPairs: Pair<String, PsiElement>): Nothing {
        error(buildString {
            append("Error: $message\n")

            for ((name, element) in errorPairs)
                append("\t$name: ${element.text} [${element.textOffset}]\n")
        })
    }
}
