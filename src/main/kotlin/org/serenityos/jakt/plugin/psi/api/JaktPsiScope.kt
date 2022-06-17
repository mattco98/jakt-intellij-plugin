package org.serenityos.jakt.plugin.psi.api

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.serenityos.jakt.plugin.project.JaktProjectService
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.psi.declaration.JaktGeneric
import org.serenityos.jakt.utils.ancestorsOfType

interface JaktPsiScope : JaktPsiElement {
    fun getDeclarations(): List<JaktDeclaration>

    fun findDeclarationIn(name: String): JaktDeclaration? {
        (this as? JaktDeclaration)?.resolveName(name)?.let { return it }

        if (this is JaktGeneric)
            getDeclGenericBounds().find { it.name == name }?.let { return it }

        for (decl in getDeclarations())
            decl.resolveName(name)?.let { return it }

        return null
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
        findDeclarationIn(name)?.let { return it }

    for (scope in ancestorsOfType<JaktPsiScope>())
        scope.findDeclarationIn(name)?.let { return it }

    return project.service<JaktProjectService>().findPreludeType(name)
}
