package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktMatchCase
import org.serenityos.jakt.psi.api.JaktMatchCaseHead

abstract class JaktMatchCaseMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktMatchCase {
    override fun getDeclarations(): List<JaktDeclaration> {
        val patternBindings = matchCaseHead.matchHeadPartList.map { part ->
            // If there are any else or expression patterns, there cannot be any bindings
            // from patterns, as each part of the match head is like an 'or'
            part.matchPattern?.destructuringPartList?.map { it.destructuringBinding } ?: return emptyList()
        }

        if (patternBindings.isEmpty())
            return emptyList()

        // Ensure we only declare identifiers which are present in all cases
        var validNames: Set<String>? = null

        for (bindingList in patternBindings) {
            val nameSet = bindingList.mapNotNull { it.name }.toSet()
            validNames = if (validNames != null) {
                validNames.intersect(nameSet)
            } else nameSet
        }

        val filteredBindings = patternBindings.map { bindings ->
            bindings.filter { it.name != null && it.name in validNames!! }
        }

        // Return the first declaration of the name
        return filteredBindings.flatten().distinctBy { it.name }
    }
}
