package org.serenityos.jakt.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktTryExpression
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.type.OptionalType
import org.serenityos.jakt.type.PrimitiveType
import org.serenityos.jakt.type.Type

abstract class JaktTryExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktTryExpression {
    override val jaktType: Type
        get() = expression?.jaktType?.let(::OptionalType) ?: PrimitiveType.Void

    override fun getDeclarations(): List<JaktDeclaration> = listOfNotNull(catchDecl)
}
