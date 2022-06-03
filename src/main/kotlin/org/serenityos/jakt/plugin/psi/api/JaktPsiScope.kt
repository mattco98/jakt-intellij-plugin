package org.serenityos.jakt.plugin.psi.api

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.serenityos.jakt.plugin.psi.JaktPsiElement
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner
import org.serenityos.jakt.plugin.psi.reference.JaktPlainQualifierMixin
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
        val index = from?.let { el ->
            children.indexOf(el).also {
                // If this is true, likely the parse tree is completely broken.
                // This has been observed to happen when typing
                if (it == -1)
                    return null
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
