package org.serenityos.jakt.plugin.psi.api

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.psi.declaration.JaktGeneric
import org.serenityos.jakt.plugin.psi.reference.JaktPlainQualifierMixin

interface JaktPsiScope : JaktPsiElement {
    fun findDeclarationIn(name: String, from: PsiElement?): JaktDeclaration? {
        val index = from?.let { el ->
            children.indexOf(el).also {
                // If this is true, likely the parse tree is completely broken.
                // This has been observed to happen when typing
                if (it == -1)
                    return null
            }
        } ?: 0

        if (this is JaktGeneric) {
            getDeclGenericBounds().find { it.name == name }?.let { return it }
        }

        for (child in children.take(index)) {
            if (child is JaktDeclaration && child.name == name)
                return child
        }

        return null
    }

    fun findDeclarationInOrAbove(name: String, from: PsiElement?): JaktDeclaration? {
        return findDeclarationIn(name, from) ?: containingScope?.findDeclarationInOrAbove(name, this)
    }

    fun findReferencesInOrBelow(element: PsiElement, name: String, from: PsiElement? = null): List<PsiElement> {
        val index = from?.let { el ->
            children.indexOf(el).also {
                require(it != -1)
            }
        } ?: 0

        val references = mutableListOf<PsiElement>()

        for (child in children.drop(index + 1)) {
            // If this shadows the declaration we're looking for, this scope cannot
            // have any more references to the element
            if (child is JaktDeclaration && child.name == name)
                return references

            references.addAll(PsiTreeUtil.findChildrenOfType(child, JaktPlainQualifierMixin::class.java)
                .filter { el ->
                    el.reference.multiResolve(false).any { it.element == element }
                })
        }

        return references
    }
}

val JaktPsiElement.containingScope: JaktPsiScope?
    get() {
        var element: PsiElement? = parent
        while (element != null && element !is JaktPsiScope)
            element = element.parent
        return element as? JaktPsiScope
    }

fun JaktPsiElement.findDeclarationInOrAbove(name: String): JaktDeclaration? {
    if (this is JaktPsiScope)
        return findDeclarationInOrAbove(name, null)

    var element: PsiElement = this
    var parent: PsiElement = parent
    while (parent !is JaktPsiScope) {
        element = parent
        parent = parent.parent
    }

    return parent.findDeclarationInOrAbove(name, element)
}
