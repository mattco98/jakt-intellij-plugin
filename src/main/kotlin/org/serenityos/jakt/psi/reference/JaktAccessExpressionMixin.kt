package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktAccessExpression
import org.serenityos.jakt.psi.caching.resolveCache
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*

abstract class JaktAccessExpressionMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktAccessExpression {
    override val jaktType: Type
        get() = resolveCache().resolveWithCaching(this) {
            val baseType = TypeInference.inferType(expression).resolveToBuiltinType(project)

            if (decimalLiteral != null) {
                if (baseType is TupleType) {
                    baseType.types.getOrNull(decimalLiteral!!.text.toInt()) ?: UnknownType
                } else UnknownType
            } else {
                val name = identifier!!.text

                BoundType.withInner(baseType) {
                    when (it) {
                        is StructType -> it.fields[name]
                            ?: it.methods[name]?.takeIf(FunctionType::hasThis)
                            ?: UnknownType
                        is EnumVariantType -> it.parent.methods[name]?.takeIf(FunctionType::hasThis) ?: UnknownType
                        is EnumType -> it.methods[name]?.takeUnless(FunctionType::hasThis) ?: UnknownType
                        else -> UnknownType
                    }
                }
            }
        }

    override fun getNameIdentifier() = identifier

    override fun getReference() = singleRef {
        if (decimalLiteral != null)
            return@singleRef null

        val baseType = TypeInference.inferType(expression).resolveToBuiltinType(project)
        val baseDecl = baseType.psiElement ?: return@singleRef null
        JaktResolver(baseDecl).findDeclaration(name!!, JaktResolver.INSTANCE)
    }
}