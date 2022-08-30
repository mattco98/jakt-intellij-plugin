package org.serenityos.jakt.psi.reference

import com.intellij.lang.ASTNode
import org.serenityos.jakt.psi.JaktTypeable
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

            val type = getTypeOfMember(baseType, decimalLiteral?.text?.toInt() ?: identifier?.text.orEmpty())

            if (didUnwrap) OptionalType(type) else type
        }

    override fun getNameIdentifier() = identifier

    override fun getReference() = singleRef {
        if (decimalLiteral != null)
            return@singleRef null

        val baseType = expression.jaktType.resolveToBuiltinType(project)
        var baseDecl = baseType.psiElement ?: return@singleRef null

        while (true) {
            JaktResolver(baseDecl).findDeclaration(name!!, JaktResolver.INSTANCE)?.let { return@singleRef it }
            baseDecl = ((baseDecl as? JaktTypeable)?.jaktType as? StructType)?.superType?.psiElement ?: break
        }

        null
    }

    private fun getTypeOfMember(type: Type, member: Any /* Int | String */): Type {
        // TODO: This is so ugly
        fun impl(type: Type): Type {
            if (member is Int) {
                return BoundType.withInner(type) {
                    (it as? TupleType)?.types?.getOrNull(member) ?: UnknownType
                }
            }

            require(member is String)

            return BoundType.withInner(type.resolveToBuiltinType(project)) {
                when (it) {
                    is StructType -> it.fields[name]
                        ?: it.methods[name]?.takeIf(FunctionType::hasThis)
                        ?: UnknownType

                    is EnumVariantType -> (it.parentType as? EnumType)?.methods?.get(name)
                        ?.takeIf(FunctionType::hasThis)
                        ?: UnknownType

                    is EnumType -> it.methods[name]?.takeIf(FunctionType::hasThis) ?: UnknownType
                    else -> UnknownType
                }
            }
        }

        return generateSequence(type) { (it as? StructType)?.superType }
            .map(::impl)
            .find { it != UnknownType }
            ?: UnknownType
    }
}
