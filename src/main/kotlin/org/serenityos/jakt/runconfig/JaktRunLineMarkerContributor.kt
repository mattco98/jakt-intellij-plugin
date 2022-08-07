package org.serenityos.jakt.runconfig

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.declaration.isMainFunction

class JaktRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != JaktTypes.IDENTIFIER)
            return null

        val funcDecl = element.ancestorOfType<JaktFunction>() ?: return null
        if (!funcDecl.isMainFunction || funcDecl.identifier != element)
            return null

        val actions = ExecutorAction.getActions(0)
        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { el -> actions.mapNotNull { getText(it, el) }.joinToString(separator = "\n") },
            *actions,
        )
    }
}
