package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktVariableDecl
import org.intellij.sdk.language.psi.JaktVariableDeclarationStatement
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.caching.resolveCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.TupleType
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.UnknownType

abstract class JaktVariableDeclMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktVariableDecl {
    override val jaktType: Type
        get() = resolveCache().resolveWithCaching(this) {
            val statement = ancestorOfType<JaktVariableDeclarationStatement>() ?: return@resolveWithCaching UnknownType
            val exprType = statement.expression.jaktType

            when {
                statement.parenOpen == null -> exprType
                exprType is TupleType -> {
                    val thisIndex = statement.variableDeclList.indexOf(this)
                    if (thisIndex == -1 || thisIndex > exprType.types.size) {
                        UnknownType
                    } else {
                        exprType.types[thisIndex]
                    }
                }
                else -> UnknownType
            }
        }
}
