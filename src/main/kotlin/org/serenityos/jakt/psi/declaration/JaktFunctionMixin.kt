package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.psi.JaktScope
import org.serenityos.jakt.psi.ancestorOfType
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.caching.JaktModificationBoundary
import org.serenityos.jakt.psi.caching.JaktModificationTracker
import org.serenityos.jakt.psi.jaktType
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.stubs.JaktFunctionStub
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktFunctionMixin : JaktStubbedNamedElement<JaktFunctionStub>, JaktFunction, JaktModificationBoundary {
    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktFunctionStub, type: IStubElementType<*, *>) : super(stub, type)

    override val tracker = JaktModificationTracker()

    override val jaktType by recursivelyGuarded<Type> {
        val linkage = if (isExtern) Linkage.External else Linkage.Internal
        val parameters = mutableListOf<FunctionType.Parameter>()

        producer {
            parameters.clear()

            val typeParameters = if (genericBounds != null) {
                getDeclGenericBounds().map { TypeParameter(it.identifier.text) }
            } else emptyList()

            val thisParam = parameterList.thisParameter

            FunctionType(
                identifier?.text,
                typeParameters,
                parameters,
                PrimitiveType.Void,
                functionReturnType.throwsKeyword != null,
                linkage,
                thisParam != null,
                thisParam?.mutKeyword != null,
            ).also {
                it.psiElement = this@JaktFunctionMixin
            }
        }

        initializer { type ->
            parameters.addAll(parameterList.parameterList.map {
                FunctionType.Parameter(
                    it.identifier.text,
                    it.typeAnnotation.jaktType,
                    it.anonKeyword != null,
                    it.mutKeyword != null,
                )
            })

            val func = type as FunctionType

            func.returnType = functionReturnType.type?.jaktType ?: expression?.jaktType ?: PrimitiveType.Void
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> = parameterList.parameterList

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList.orEmpty()
}

val JaktFunction.isExtern: Boolean
    get() = externKeyword != null

val JaktFunction.isTopLevel: Boolean
    get() = ancestorOfType<JaktScope>() is JaktFile

val JaktFunction.returnType: Type
    get() = (jaktType as FunctionType).returnType

val JaktFunction.isMainFunction: Boolean
    get() = isTopLevel && !isExtern && name == "main" && returnType.let {
        it == PrimitiveType.Void || it == PrimitiveType.CInt
    } && parameterList.let {
        it.thisParameter == null && if (it.parameterList.size == 1) {
            val type = it.parameterList.single().jaktType
            type is ArrayType && type.underlyingType == PrimitiveType.String
        } else it.parameterList.isEmpty()
    }
