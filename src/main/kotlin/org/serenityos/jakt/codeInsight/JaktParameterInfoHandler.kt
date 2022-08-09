package org.serenityos.jakt.codeInsight

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandler
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktArgumentList
import org.serenityos.jakt.psi.api.JaktCallExpression
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktParameterList
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.render.renderType

class JaktParameterInfoHandler : ParameterInfoHandler<JaktArgumentList, JaktParameterInfoHandler.ParameterInfo> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): JaktArgumentList? {
        val argList = findElement(context.file, context.offset) ?: return null
        val callTarget = argList.ancestorOfType<JaktCallExpression>()?.expression ?: return null
        val func = callTarget.jaktType.psiElement as? JaktFunction ?: return null
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
        val index = parameterOwner.argumentList.indexOfFirst {
            it.startOffset > context.offset
        }

        if (index != -1)
            context.setCurrentParameter(index)
    }

    override fun showParameterInfo(element: JaktArgumentList, context: CreateParameterInfoContext) {
        context.showHint(element, element.startOffset, this)
    }

    class ParameterInfo(parameters: JaktParameterList) {
        val parameters = parameters.parameterList.map {
            val anon = if (it.anonKeyword != null) "anon " else ""
            val mut = if (it.mutKeyword != null) "mut " else ""
            val type = renderType(it.jaktType)

            "$anon$mut${it.name}: $type"
        }

        fun findParameterRange(parameter: Int): TextRange {
            if (parameter !in parameters.indices)
                return TextRange.EMPTY_RANGE

            val start = parameters.take(parameter).sumOf { it.length + 2 }
            return TextRange.create(start, start + parameters[parameter].length)
        }
    }
}
