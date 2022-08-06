package org.serenityos.jakt.structure

import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktLanguage
import org.serenityos.jakt.render.renderType

class JaktBreadcrumbsInfoProvider : BreadcrumbsProvider {
    override fun getLanguages() = arrayOf(JaktLanguage)

    override fun acceptElement(element: PsiElement) = getElementInfoImpl(element) != null

    override fun getElementInfo(element: PsiElement) = getElementInfoImpl(element)!!

    fun getElementInfoImpl(element: PsiElement) = when (element) {
        is JaktFunction -> element.name + "()"
        is JaktStructField -> element.name
        is JaktEnumDeclaration -> renderType(element.jaktType)
        is JaktStructDeclaration -> renderType(element.jaktType)
        is JaktNamespaceDeclaration -> "namespace ${element.name}"
        else -> null
    }
}
