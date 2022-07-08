package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktExpression
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.serenityos.jakt.psi.findChildOfType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.TypeInference
import org.serenityos.jakt.type.UnknownType

abstract class JaktVariableDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktVariableDeclarationStatement {
    override val jaktType: Type
        get() = if (typeAnnotation != null) {
            typeAnnotation!!.jaktType
        } else {
            findChildOfType<JaktExpression>()?.let(TypeInference::inferType) ?: UnknownType
        }
}
