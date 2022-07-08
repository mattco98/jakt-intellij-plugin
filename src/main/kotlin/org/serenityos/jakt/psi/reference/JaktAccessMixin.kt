package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktAccess
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.caching.resolveCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*

abstract class JaktAccessMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktAccess {
    override val jaktType: Type
        get() = resolveCache().resolveWithCaching(this) {
            val accessExpr = ancestorOfType<JaktAccessExpression>()!!
            val baseType = TypeInference.inferType(accessExpr.expression).resolveToBuiltinType(project)

            if (decimalLiteral != null) {
                if (baseType is TupleType) {
                    baseType.types.getOrNull(decimalLiteral!!.text.toInt()) ?: UnknownType
                } else UnknownType
            } else {
                val name = identifier!!.text

                when (baseType) {
                    is StructType -> baseType.fields[name]
                        ?: baseType.methods[name]?.takeIf { it.hasThis }
                        ?: UnknownType
                    is EnumVariantType -> baseType.parent.methods[name]?.takeIf { it.hasThis } ?: UnknownType
                    is EnumType -> baseType.methods[name]?.takeIf { !it.hasThis } ?: UnknownType
                    else -> UnknownType
                }
            }
        }

    override fun getNameIdentifier() = identifier

    override fun getReference() = singleRef {
        if (decimalLiteral != null)
            return@singleRef null

        val accessExpr = ancestorOfType<JaktAccessExpression>()!!
        val baseType = TypeInference.inferType(accessExpr.expression).resolveToBuiltinType(project)
        val baseDecl = (baseType as? DeclarationType)?.psiElement ?: return@singleRef null
        JaktResolver(baseDecl).findDeclaration(name!!, JaktResolver.INSTANCE)
    }
}
