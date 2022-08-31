package org.serenityos.jakt.lineMarkers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.declaration.isOverride
import org.serenityos.jakt.type.StructType

class JaktChildMethodLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.elementType != JaktTypes.IDENTIFIER)
            return null

        val functionDecl = element.parent as? JaktFunction ?: return null
        if (!functionDecl.isOverride)
            return null

        val structParent = functionDecl.ancestorOfType<JaktStructDeclaration>() ?: return null

        return NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridingMethod)
            .setTargets(NotNullLazyValue.createValue {
                // TODO: Show all methods in the hierarchy?
                val superType = structParent.superType?.type?.jaktType as? StructType ?: return@createValue emptyList()
                listOfNotNull(superType.methods[element.text]?.psiElement)
            })
            .setCellRenderer {
                object : PsiElementListCellRenderer<PsiElement>() {
                    override fun getElementText(element: PsiElement): String {
                        return element.ancestorOfType<JaktStructDeclaration>()?.name.orEmpty()
                    }

                    override fun getContainerText(element: PsiElement, name: String) =
                        element.containingFile?.virtualFile?.name
                }
            }
            .setTooltipText("Find declarations")
            .createLineMarkerInfo(element)
    }
}
