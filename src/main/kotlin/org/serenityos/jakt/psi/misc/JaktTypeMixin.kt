package org.serenityos.jakt.psi.misc

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.psi.api.JaktTypeable
import org.serenityos.jakt.type.*

abstract class JaktTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), JaktTypeable {
    override val jaktType: Type
        get() = when (this) {
            is JaktRawType -> RawType(type!!.jaktType)
            is JaktWeakType -> WeakType(type!!.jaktType)
            is JaktOptionalType -> OptionalType(type.jaktType)
            is JaktArrayType -> ArrayType(type.jaktType)
            is JaktDictionaryType -> DictionaryType(typeList[0].jaktType, typeList[1].jaktType)
            is JaktSetType -> SetType(type!!.jaktType)
            is JaktTupleType -> TupleType(typeList.map { it.jaktType })
            is JaktTypeAnnotation -> type.jaktType
            else -> error("Unknown Type class ${this::class.simpleName}")
        }
}
