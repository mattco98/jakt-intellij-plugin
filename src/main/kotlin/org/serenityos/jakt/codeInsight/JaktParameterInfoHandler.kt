package org.serenityos.jakt.codeInsight

import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import org.intellij.sdk.language.psi.JaktArgumentList
import org.intellij.sdk.language.psi.JaktCallExpression
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktParameterList
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.type.Type

class JaktParameterInfoHandler : ParameterInfoHandler<JaktArgumentList, JaktParameterInfoHandler.ParameterInfo> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): JaktArgumentList? {
        val argList = findElement(context.file, context.offset) ?: return null
        val callTarget = argList.ancestorOfType<JaktCallExpression>()?.expression ?: return null
        val func = (callTarget.jaktType as? Type.TopLevelDecl)?.declaration as? JaktFunctionDeclaration ?: return null
        context.itemsToShow = arrayOf(ParameterInfo(func.parameterList))
        return argList
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): JaktArgumentList? {
        return findElement(context.file, context.offset)
    }

    private fun findElement(file: PsiFile, offset: Int): JaktArgumentList? {
        return file.findElementAt(offset)?.ancestorOfType<JaktArgumentList>()
    }

    override fun updateUI(p: ParameterInfo, context: ParameterInfoUIContext) {
        val range = p.findParameterRange(context.currentParameterIndex)
        context.setupUIComponentPresentation(
            p.parameters.joinToString().ifEmpty { "<no parameters>" },
            range.startOffset,
            range.endOffset,
            !context.isUIComponentEnabled,
            false,
            false,
            context.defaultParameterColor,
        )
    }

    override fun updateParameterInfo(parameterOwner: JaktArgumentList, context: UpdateParameterInfoContext) {
        val currentParameterIndex = ParameterInfoUtils.getCurrentParameterIndex(
            parameterOwner.node,
            context.offset,
            JaktTypes.MEMBER_SEPARATOR,
        )

        context.setCurrentParameter(currentParameterIndex)
    }

    override fun showParameterInfo(element: JaktArgumentList, context: CreateParameterInfoContext) {
        context.showHint(element, element.startOffset, this)
    }

    class ParameterInfo(parameters: JaktParameterList) {
        val parameters = parameters.parameterList.map {
            val anon = if (it.anonKeyword != null) "anon " else ""
            val mut = if (it.mutKeyword != null) "mut " else ""

            "$anon$mut${it.name}: ${it.jaktType.typeRepr()}"
        }

        fun findParameterRange(parameter: Int): TextRange {
            if (parameter !in parameters.indices)
                return TextRange.EMPTY_RANGE

            val start = parameters.take(parameter).sumOf { it.length + 2 }
            return TextRange.create(start, start + parameters[parameter].length)
        }
    }
}
