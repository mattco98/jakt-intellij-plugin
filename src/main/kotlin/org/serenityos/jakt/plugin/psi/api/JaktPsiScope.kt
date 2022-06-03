package org.serenityos.jakt.plugin.psi.api

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.psi.reference.JaktPsiReference

interface JaktPsiScope : JaktPsiElement {
    fun declarations(): List<JaktDeclaration> {
        return CachedValuesManager.getCachedValue(this, DECLARATIONS_KEY) {
            val values = children.filterIsInstance<JaktDeclaration>() +
                children.filterIsInstance<JaktPsiScope>().flatMap(JaktPsiScope::declarations)
            CachedValueProvider.Result(values, modificationBoundary)
        }
    }

    fun findDeclarationIn(name: String, from: PsiElement?): JaktDeclaration? {
        val t = this
        val index = from?.let { el ->
            children.indexOf(el).also {
                require(it != -1)
            }
        } ?: 0

        for (child in children.take(index)) {
            if (child is JaktDeclaration && child.name == name)
                return child
        }

        return null
    }

    fun findDeclarationInOrAbove(name: String, from: PsiElement?): JaktDeclaration? {
        return findDeclarationIn(name, from) ?: containingScope?.findDeclarationInOrAbove(name, this)
    }

    fun findReferencesInOrBelow(name: String, from: PsiElement? = null): List<JaktPsiReference> {
        val index = from?.let { el ->
            children.indexOf(el).also {
                require(it != -1)
            }
        } ?: 0

        val references = mutableListOf<JaktPsiReference>()

        for (child in children.drop(index)) {
            // If this shadows the declaration we're looking for, this scope cannot
            // have any more references to the element
            if (child is JaktDeclaration && child.name == name)
                return references
            if (child is JaktPsiReference && child.element.name == name)
                references.add(child)
            if (child is JaktPsiScope)
                references.addAll(child.findReferencesInOrBelow(name, null))
        }

        return references
    }

    companion object {
        private val DECLARATIONS_KEY = Key.create<CachedValue<List<JaktDeclaration>>>("DECLARATIONS_KEY")
    }
}

val JaktPsiElement.containingScope: JaktPsiScope?
    get() {
        var element: PsiElement? = parent
        while (element != null && element !is JaktPsiScope)
            element = element.parent
        return element as? JaktPsiScope
    }
