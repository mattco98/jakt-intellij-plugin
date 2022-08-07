package org.serenityos.jakt

import com.intellij.codeInsight.hint.DeclarationRangeHandler
import com.intellij.openapi.util.TextRange
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.JaktFunction
import org.intellij.sdk.language.psi.JaktNamespaceDeclaration
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.serenityos.jakt.psi.JaktPsiElement

class JaktStructDeclRangeHandler : DeclarationRangeHandler<JaktStructDeclaration> {
    override fun getDeclarationRange(container: JaktStructDeclaration): TextRange? = with(container) {
        val start = externKeyword ?: classKeyword ?: structKeyword!!
        val end = genericBounds ?: identifier
        TextRange.create(start.textRange.startOffset, end.textRange.endOffset)
    }

}

class JaktDeclarationRangeHandler : DeclarationRangeHandler<JaktPsiElement> {
    override fun getDeclarationRange(container: JaktPsiElement): TextRange? = with(container) {
        val (startEl, endEl) = when (this) {
            is JaktStructDeclaration -> {
                val start = externKeyword ?: classKeyword ?: structKeyword!!
                val end = genericBounds ?: identifier
                start to end
            }
            is JaktEnumDeclaration -> {
                val start = boxedKeyword ?: enumKeyword
                val end = underlyingTypeEnumBody?.typeAnnotation
                    ?: normalEnumBody?.genericBounds
                    ?: identifier

                start to end
            }
            is JaktNamespaceDeclaration -> namespaceKeyword to identifier
            is JaktFunction -> {
                val start = externKeyword ?: functionKeyword
                val end = functionReturnType.let { it.type ?: it.throwsKeyword }
                    ?: parameterList.parenClose
                start to end
            }
            else -> return null
        }

        return TextRange.create(startEl.textRange.startOffset, endEl.textRange.endOffset)
    }
}
