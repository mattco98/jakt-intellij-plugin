package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.api.JaktAccessExpression
import org.serenityos.jakt.psi.caching.typeCache
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.*

abstract class JaktAccessExpressionMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktAccessExpression {
    override val jaktType: Type
        get() = typeCache().resolveWithCaching(this) {
            val (baseType, didUnwrap) = expression.jaktType.let {
                if (it is OptionalType && dotQuestionMark != null) {
                    it.underlyingType to true
                } else it to false
            }

            // TODO: This is so ugly
            val type = if (decimalLiteral != null) {
                BoundType.withInner(baseType) {
                    (it as? TupleType)?.types?.getOrNull(decimalLiteral!!.text.toInt()) ?: UnknownType
                }
            } else {
                val name = identifier?.text

                BoundType.withInner(baseType.resolveToBuiltinType(project)) {
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

            if (didUnwrap) OptionalType(type) else type
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
