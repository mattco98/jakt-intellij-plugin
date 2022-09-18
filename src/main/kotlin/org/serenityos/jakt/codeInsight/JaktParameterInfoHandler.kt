package org.serenityos.jakt.codeInsight

import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.*
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.render.renderType

class JaktParameterInfoHandler : ParameterInfoHandler<JaktArgumentList, JaktParameterInfoHandler.ParameterInfo> {
    override fun findElementForParameterInfo(context: CreateParameterInfoContext): JaktArgumentList? {
        val argList = findElement(context.file, context.offset) ?: return null
        val callTarget = argList.ancestorOfType<JaktCallExpression>()?.expression ?: return null

        val paramInfo = when (val element = callTarget.jaktType.psiElement) {
            is JaktFunction -> FunctionParameterInfo(element.parameterList)
            is JaktStructDeclaration -> StructParameterInfo(element)
            else -> null
        }
        context.itemsToShow = arrayOf(paramInfo)

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
            p.toString().ifEmpty { "<no parameters>" },
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

    abstract class ParameterInfo {
        protected abstract val parameters: List<Parameter>

        fun findParameterRange(parameter: Int): TextRange {
            if (parameter !in parameters.indices)
                return TextRange.EMPTY_RANGE

            val start = parameters.take(parameter).sumOf { it.toString().length + 2 }
            return TextRange.create(start, start + parameters[parameter].toString().length)
        }

        override fun toString() = parameters.joinToString()

        data class Parameter(val name: String, val type: String, val prefix: String = "") {
            override fun toString() = "$prefix$name: $type"
        }
    }

    class StructParameterInfo(struct: JaktStructDeclaration) : ParameterInfo() {
        override val parameters = sequence {
            var decl = struct

            while (true) {
                yieldAll(decl.structBody.structMemberList.mapNotNull { it.structField })
                decl = decl.superType?.type?.jaktType?.psiElement as? JaktStructDeclaration ?: return@sequence
            }
        }.map {
            Parameter(it.name ?: "", renderType(it.jaktType))
        }.toList().asReversed()
    }

    class FunctionParameterInfo(parameters: JaktParameterList) : ParameterInfo() {
        override val parameters = parameters.parameterList.map {
            val anon = if (it.anonKeyword != null) "anon " else ""
            val mut = if (it.mutKeyword != null) "mut " else ""
            Parameter(it.name ?: "", renderType(it.jaktType), "$anon$mut")
        }
    }
}
