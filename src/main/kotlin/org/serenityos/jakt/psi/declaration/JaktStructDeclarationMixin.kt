package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.index.JaktPath
import org.serenityos.jakt.index.toPath
import org.serenityos.jakt.psi.JaktADT
import org.serenityos.jakt.psi.api.JaktFunction
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.api.JaktStructField
import org.serenityos.jakt.psi.api.JaktStructMethod
import org.serenityos.jakt.psi.greenStub
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.psi.reference.function
import org.serenityos.jakt.stubs.JaktStructDeclarationStub
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktStructDeclarationMixin : JaktStubbedNamedElement<JaktStructDeclarationStub>, JaktStructDeclaration, JaktADT {
    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktStructDeclarationStub, type: IStubElementType<*, *>) : super(stub, type)

    override val jaktType by recursivelyGuarded<StructType> {
        val typeParameters = mutableListOf<TypeParameter>()
        val fields = mutableMapOf<String, Type>()
        val methods = mutableMapOf<String, FunctionType>()

        producer {
            typeParameters.clear()
            fields.clear()
            methods.clear()

            val linkage = if (isExtern) Linkage.External else Linkage.Internal

            StructType(
                name,
                typeParameters,
                fields,
                methods,
                null,
                isClass,
                linkage,
            ).also {
                it.psiElement = this@JaktStructDeclarationMixin
            }
        }

        initializer { structType ->
            if (genericBounds != null)
                typeParameters.addAll(getDeclGenericBounds().map { it.jaktType as TypeParameter })

            structType.superType = superType?.type?.jaktType

            // TODO: Visibility
            val members = structBody.structMemberList.map { it.structMethod ?: it.structField }

            members.filterIsInstance<JaktStructField>().forEach {
                fields[it.identifier.text] = it.typeAnnotation.jaktType
            }

            members.filterIsInstance<JaktStructMethod>().forEach { method ->
                val type = method.function?.jaktType as? FunctionType ?: return@forEach
                methods[type.name!!] = type
            }
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        return structBody.structMemberList.mapNotNull { it.structField ?: it.structMethod?.function }
    }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList.orEmpty()

    override fun getMethods(): List<JaktFunction> {
        return structBody.structMemberList.mapNotNull { it.structMethod?.function }
    }

    override fun getSuperElement(): JaktADT? {
        return superType?.type?.jaktType?.psiElement as? JaktADT
    }

    override fun getBodyStartAnchor() = structBody.curlyOpen
}

val JaktStructDeclaration.isExtern: Boolean
    get() = greenStub?.isExtern ?: (externKeyword != null)

val JaktStructDeclaration.isClass: Boolean
    get() = greenStub?.isClass ?: (classKeyword != null)

val JaktStructDeclaration.parentPath: JaktPath?
    get() = greenStub?.parentPath ?: (superType?.type?.jaktType?.psiElement as? JaktDeclaration)?.toPath()
