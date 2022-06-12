package org.serenityos.jakt.plugin.psi.api

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktVisitor
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

    fun findReferencesInOrBelow(name: String, from: PsiElement? = null): List<JaktPsiElement> {
        val index = from?.let { el ->
            children.indexOf(el).also {
                require(it != -1)
            }
        } ?: 0

        val references = mutableListOf<JaktPsiElement>()

        PsiTreeUtil.processElements({
            when {
                it is JaktDeclaration && it.name == name -> {
                    // If this shadows the declaration we're looking for, this scope cannot
                    // have any more references to the element
                    return@processElements false
                }
                it is JaktPsiScope -> references.addAll(it.findReferencesInOrBelow(name))
                it is JaktPlainQualifier && it.name == name -> references.add(it)
            }

            true
        }, *children.drop(index + 1).toTypedArray())

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
