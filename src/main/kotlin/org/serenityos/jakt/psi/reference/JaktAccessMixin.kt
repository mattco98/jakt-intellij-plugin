package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktAccess
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.JaktResolver
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.TypeInference
import org.serenityos.jakt.type.resolveToBuiltinType

abstract class JaktAccessMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktAccess {
    override val jaktType: Type
        get() = (reference.resolve() as? JaktTypeable)?.jaktType ?: Type.Unknown

    override fun getNameIdentifier() = identifier

    override fun getReference() = singleRef {
        if (decimalLiteral != null)
            return@singleRef null

        val accessExpr = ancestorOfType<JaktAccessExpression>()!!
        val baseType = TypeInference.inferType(accessExpr.expression).resolveToBuiltinType(project)
        val baseDecl = (baseType as? Type.TopLevelDecl)?.declaration ?: return@singleRef null
        JaktResolver(baseDecl).findDeclaration(name!!, JaktResolver.INSTANCE)
    }
}
