package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.JaktFile
import org.serenityos.jakt.JaktTypes
import org.serenityos.jakt.psi.*
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.caching.JaktModificationBoundary
import org.serenityos.jakt.psi.caching.JaktModificationTracker
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.stubs.JaktFunctionStub
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktFunctionMixin : JaktStubbedNamedElement<JaktFunctionStub>, JaktFunction, JaktModificationBoundary {
    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktFunctionStub, type: IStubElementType<*, *>) : super(stub, type)

    override val tracker = JaktModificationTracker()

    override val jaktType by recursivelyGuarded<Type> {
        val parameters = mutableListOf<FunctionType.Parameter>()
        val typeParameters = mutableListOf<TypeParameter>()

        producer {
            parameters.clear()
            typeParameters.clear()

            val linkage = if (isExtern) Linkage.External else Linkage.Internal

            FunctionType(
                name.ifBlank { null },
                typeParameters,
                parameters,
                PrimitiveType.Void,
                throws,
                linkage,
                hasThis,
                thisIsMutable,
            ).also {
                it.psiElement = this@JaktFunctionMixin
            }
        }

        initializer { type ->
            if (genericBounds != null)
                typeParameters.addAll(getDeclGenericBounds().map { TypeParameter(it.identifier.text) })

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

val JaktFunction.externKeyword: PsiElement?
    get() = findChildOfType(JaktTypes.EXTERN_KEYWORD)

val JaktFunction.virtualKeyword: PsiElement?
    get() = findChildOfType(JaktTypes.VIRTUAL_KEYWORD)

val JaktFunction.overrideKeyword: PsiElement?
    get() = findChildOfType(JaktTypes.OVERRIDE_KEYWORD)

val JaktFunction.isExtern: Boolean
    get() = greenStub?.isExtern ?: (externKeyword != null)

val JaktFunction.hasThis: Boolean
    get() = greenStub?.hasThis ?: (parameterList.thisParameter != null)

val JaktFunction.thisIsMutable: Boolean
    get() = greenStub?.thisIsMutable ?: (parameterList.thisParameter?.mutKeyword != null)

val JaktFunction.throws: Boolean
    get() = greenStub?.throws ?: (functionReturnType.throwsKeyword != null)

val JaktFunction.isVirtual: Boolean
    get() = greenStub?.isVirtual ?: (virtualKeyword != null)

val JaktFunction.isOverride: Boolean
    get() = greenStub?.isOverride ?: (overrideKeyword != null)

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
