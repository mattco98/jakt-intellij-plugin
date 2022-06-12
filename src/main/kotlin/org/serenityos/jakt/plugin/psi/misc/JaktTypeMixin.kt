package org.serenityos.jakt.plugin.psi.misc

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.api.containingScope
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktTypeable {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val type = when (this) {
                is JaktRawType -> Type.Raw(type!!.jaktType)
                is JaktWeakType -> Type.Weak(type!!.jaktType)
                is JaktPlainType -> resolvePlainType(this)
                is JaktOptionalType -> Type.Optional(type.jaktType)
                is JaktArrayType -> Type.Array(type.jaktType)
                is JaktDictionaryType -> Type.Dictionary(typeList[0].jaktType, typeList[1].jaktType)
                is JaktSetType -> Type.Set(type!!.jaktType)
                is JaktTupleType -> Type.Tuple(typeList.map { it.jaktType })
                is JaktTypeAnnotation -> type.jaktType
                else -> error("Unknown Type class ${this::class.simpleName}")
            }

            // TODO: Smarter caching
            CachedValueProvider.Result(type, this)
        }

    companion object {
        private fun resolvePlainType(element: JaktPlainType): Type {
            val idents = element.findChildrenOfType(JaktTypes.IDENTIFIER).map { it.text }
            require(idents.size == 1) {
                "TODO: Resolve namespace types"
            }

            val name = idents.last()

            val primitive = Type.Primitive.values().find { it.typeRepr() == name }
            if (primitive != null)
                return primitive

            return element.containingScope?.findDeclarationInOrAbove(name, null)?.jaktType ?: return Type.Unknown
        }
    }
}
