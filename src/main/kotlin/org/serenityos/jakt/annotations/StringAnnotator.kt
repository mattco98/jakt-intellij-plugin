package org.serenityos.jakt.annotations

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.intellij.sdk.language.psi.JaktArgument
import org.intellij.sdk.language.psi.JaktCallExpression
import org.intellij.sdk.language.psi.JaktPlainQualifierExpression
import org.intellij.sdk.language.psi.JaktUnlabeledArgument
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.syntax.Highlights
import org.serenityos.jakt.type.TypeInference

object StringAnnotator : JaktAnnotator() {
    @Suppress("RegExpRepeatedSpace")
    private val INTERPOLATION_REGEX = """
        (?<!\{)     # Ensure no open brace before matched open brace
        \{          # The open brace
        (?!\{)      # Ensure no open brace after matched open brace. Checks behind and ahead are
                    #   necessary since we use a dot qualifier for the content, which would capture
                    #   any braces otherwise
        (.*?)       # The format specifier content, used to check for index references
        (?<!})      # Ensure no close brace before matched close brace
        }           # The close brace
        (?!})       # Ensure no close brace after matched close brace
    """.trimIndent().toRegex(RegexOption.COMMENTS)

    override fun annotate(element: PsiElement, holder: JaktAnnotationHolder) = with(holder) {
        if (element.elementType == JaktTypes.STRING_LITERAL) {
            element.highlight(Highlights.LITERAL_STRING)
            highlightFormatString(element)
        }
    }

    private fun getFormatSpecifiers(element: PsiElement): FormatStringInfo? {
        val unlabeledArgument = element.parent?.parent as? JaktUnlabeledArgument ?: return null
        val argument = unlabeledArgument.parent as? JaktArgument ?: return null
        val call = argument.parent?.parent as? JaktCallExpression ?: return null
        if (argument != call.argumentList.argumentList.firstOrNull())
            return null

        val callTarget = call.expression
        if (callTarget !is JaktPlainQualifierExpression || callTarget.reference?.resolve() != null)
            return null

        if (!TypeInference.builtinFunctionTypes.containsKey(callTarget.text))
            return null

        val matches = INTERPOLATION_REGEX.findAll(element.text)
        var implicitIndex = 0

        val specifiers = matches.mapNotNull {
            val totalMatch = it.groups[0] ?: return@mapNotNull null
            val contents = it.groups[1] ?: return@mapNotNull null

            val start = totalMatch.range.first + element.textRange.startOffset
            val end = totalMatch.range.last + element.textRange.startOffset + 1
            val index = contents.value.takeWhile(Char::isDigit).toIntOrNull() ?: implicitIndex++

            FormatSpecifier(start, end, index)
        }.toList()

        return FormatStringInfo(specifiers, call.argumentList.argumentList.size - 1)
    }

    private fun JaktAnnotationHolder.highlightFormatString(element: PsiElement) {
        val (specifiers, numArguments) = getFormatSpecifiers(element) ?: return

        for (specifier in specifiers) {
            val range = TextRange.create(specifier.start, specifier.end)

            // First, highlight the specifier
            range.highlight(Highlights.STRING_FORMAT_SPECIFIER)

            // Then check for errors
            if (specifier.argIndex >= numArguments)
                range.highlightError("Specifier refers to non-existent argument index ${specifier.argIndex}")
        }
    }

    data class FormatStringInfo(
        val specifiers: List<FormatSpecifier>,
        val numArguments: Int,
    )

    data class FormatSpecifier(
        val start: Int,
        val end: Int,
        val argIndex: Int,
    )
}
