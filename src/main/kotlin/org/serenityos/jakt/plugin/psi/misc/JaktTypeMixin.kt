package org.serenityos.jakt.plugin.psi.misc

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.plugin.type.resolvePlainType

abstract class JaktTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktTypeable {
    override val jaktType: Type
        get() = when (this) {
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
}
