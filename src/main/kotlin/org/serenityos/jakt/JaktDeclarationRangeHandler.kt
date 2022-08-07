package org.serenityos.jakt

import com.intellij.codeInsight.hint.DeclarationRangeHandler
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.serenityos.jakt.psi.api.JaktEnumDeclaration
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktNamespaceDeclaration
import org.serenityos.jakt.psi.api.JaktStructDeclaration

abstract class JaktDeclarationRangeHandler<T : PsiElement> : DeclarationRangeHandler<T> {
    abstract fun T.start(): PsiElement
    abstract fun T.end(): PsiElement

    final override fun getDeclarationRange(container: T): TextRange {
        return TextRange.create(container.start().textRange.startOffset, container.end().textRange.endOffset)
    }
}

class JaktStructDeclRangeHandler : JaktDeclarationRangeHandler<JaktStructDeclaration>() {
    override fun JaktStructDeclaration.start() = externKeyword ?: classKeyword ?: structKeyword!!
    override fun JaktStructDeclaration.end() = genericBounds ?: identifier
}

class JaktEnumDeclRangeHandler : JaktDeclarationRangeHandler<JaktEnumDeclaration>() {
    override fun JaktEnumDeclaration.start() = boxedKeyword ?: enumKeyword
    override fun JaktEnumDeclaration.end() = underlyingTypeEnumBody?.typeAnnotation
        ?: normalEnumBody?.genericBounds ?: identifier
}

class JaktNamespaceDeclRangeHandler : JaktDeclarationRangeHandler<JaktNamespaceDeclaration>() {
    override fun JaktNamespaceDeclaration.start() = namespaceKeyword
    override fun JaktNamespaceDeclaration.end() = identifier
}

class JaktFunctionDeclRangeHandler : JaktDeclarationRangeHandler<JaktFunction>() {
    override fun JaktFunction.start() = externKeyword ?: functionKeyword
    override fun JaktFunction.end() = functionReturnType.let { it.type ?: it.throwsKeyword } ?: parameterList.parenClose
}
