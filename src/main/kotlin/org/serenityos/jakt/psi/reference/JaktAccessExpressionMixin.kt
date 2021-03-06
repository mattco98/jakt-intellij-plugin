package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.psi.api.jaktType
import org.serenityos.jakt.psi.caching.typeCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*

abstract class JaktAccessExpressionMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktAccessExpression {
    override val jaktType: Type
        get() = typeCache().resolveWithCaching(this) {
            val type = expression.jaktType

            // TODO: This is so ugly
            if (decimalLiteral != null) {
                BoundType.withInner(type) {
                    (it as? TupleType)?.types?.getOrNull(decimalLiteral!!.text.toInt()) ?: UnknownType
                }
            } else {
                val name = identifier!!.text

                BoundType.withInner(type.resolveToBuiltinType(project)) {
                    when (it) {
                        is StructType -> it.fields[name]
                            ?: it.methods[name]?.takeIf(FunctionType::hasThis)
                            ?: UnknownType
                        is EnumVariantType -> it.parent.methods[name]?.takeIf(FunctionType::hasThis)
                            ?: UnknownType
                        is EnumType -> it.methods[name]?.takeIf(FunctionType::hasThis) ?: UnknownType
                        else -> UnknownType
                    }
                }
            }
        }

    override fun getNameIdentifier() = identifier

    override fun getReference() = singleRef {
        if (decimalLiteral != null)
            return@singleRef null

        val baseType = expression.jaktType.resolveToBuiltinType(project)
        val baseDecl = baseType.psiElement ?: return@singleRef null
        JaktResolver(baseDecl).findDeclaration(name!!, JaktResolver.INSTANCE)
    }
}
