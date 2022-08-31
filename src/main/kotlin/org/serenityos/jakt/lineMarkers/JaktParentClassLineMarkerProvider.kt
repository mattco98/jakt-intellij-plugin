package org.serenityos.jakt.lineMarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.index.JaktStructInheritanceIndex
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktStructDeclaration

class JaktParentClassLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.elementType != JaktTypes.IDENTIFIER)
            return null

        val structDecl = element.parent as? JaktStructDeclaration ?: return null

        if (!JaktStructInheritanceIndex.hasInheritors(structDecl))
            return null

        return NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
            .setTargets(NotNullLazyValue.createValue {
                val s = element.ancestorOfType<JaktStructDeclaration>() ?: return@createValue emptyList()
                val els = JaktStructInheritanceIndex.getInheritors(s)
                els
            })
            .setPopupTitle("Choose implementation of ${element.text}")
            .setTooltipText("Find subclasses")
            .createLineMarkerInfo(element)
    }
}
