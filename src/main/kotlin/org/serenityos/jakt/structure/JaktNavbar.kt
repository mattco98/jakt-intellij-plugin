package org.serenityos.jakt.structure

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.psi.PsiElement
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktLanguage

object JaktNavbar : StructureAwareNavBarModelExtension() {
    override val language = JaktLanguage

    override fun getPresentableText(obj: Any): String? {
        return when (obj) {
            !is PsiElement -> null
            is JaktFile -> obj.name
            else -> JaktBreadcrumbsInfoProvider.getNavInfo(obj)
        }
    }
}
