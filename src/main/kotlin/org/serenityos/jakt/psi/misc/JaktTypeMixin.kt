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
            is JaktReferenceType -> ReferenceType(type!!.jaktType, mutKeyword != null)
            is JaktArrayType -> ArrayType(type.jaktType)
            is JaktDictionaryType -> DictionaryType(typeList[0].jaktType, typeList[1].jaktType)
            is JaktSetType -> SetType(type!!.jaktType)
            is JaktTupleType -> TupleType(typeList.map { it.jaktType })
            is JaktNeverType -> PrimitiveType.Never
            is JaktVoidType -> PrimitiveType.Void
            is JaktTypeAnnotation -> type.jaktType
            is JaktFunctionType -> FunctionType(
                null,
                emptyList(),
                parameterList?.parameterList?.map {
                    FunctionType.Parameter(
                        it.identifier.text,
                        it.typeAnnotation.jaktType,
                        it.anonKeyword != null,
                        it.mutKeyword != null,
                    )
                }.orEmpty(),
                functionReturnType?.type?.jaktType ?: PrimitiveType.Void,
                functionReturnType?.throwsKeyword != null,
                Linkage.Internal,
                hasThis = false,
                thisIsMutable = false,
            )
            else -> error("Unknown Type class ${this::class.simpleName}")
        }
}
