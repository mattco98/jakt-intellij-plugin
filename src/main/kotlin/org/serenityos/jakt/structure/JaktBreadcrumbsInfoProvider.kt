package org.serenityos.jakt.structure

import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.serenityos.jakt.JaktLanguage
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.render.renderType

object JaktBreadcrumbsInfoProvider : BreadcrumbsProvider {
    override fun getLanguages() = arrayOf(JaktLanguage)

    override fun acceptElement(element: PsiElement) = getNavInfo(element) != null

    override fun getElementInfo(element: PsiElement) = getNavInfo(element)!!

    // TODO: Support if/match/loops as well for breadcrumbs
    fun getNavInfo(element: Any) = when (element) {
        is JaktFunction -> element.name + "()"
        is JaktStructField -> element.name
        is JaktEnumDeclaration -> renderType(element.jaktType)
        is JaktEnumVariant -> element.name
        is JaktStructDeclaration -> renderType(element.jaktType)
        is JaktNamespaceDeclaration -> "namespace ${element.name}"
        else -> null
    }
}
