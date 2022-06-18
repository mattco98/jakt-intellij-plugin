package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.serenityos.jakt.plugin.psi.named.JaktNamedElement
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.TypeInference
import org.serenityos.jakt.utils.findChildOfType

abstract class JaktVariableDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktVariableDeclarationStatement {
    override val jaktType: Type
        get() = if (typeAnnotation != null) {
            typeAnnotation!!.jaktType
        } else {
            findChildOfType<JaktExpression>()?.let(TypeInference::inferType) ?: Type.Unknown
        }

    override fun getExpression() = findChildOfType<JaktExpression>()
}
