package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.api.JaktStructField
import org.serenityos.jakt.psi.api.JaktStructMethod
import org.serenityos.jakt.psi.named.JaktStubbedNamedElement
import org.serenityos.jakt.psi.reference.function
import org.serenityos.jakt.stubs.JaktStructDeclarationStub
import org.serenityos.jakt.type.*
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktStructDeclarationMixin : JaktStubbedNamedElement<JaktStructDeclarationStub>, JaktStructDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: JaktStructDeclarationStub, type: IStubElementType<*, *>) : super(stub, type)

    override val jaktType by recursivelyGuarded<Type> {
        val typeParameters = mutableListOf<TypeParameter>()
        val fields = mutableMapOf<String, Type>()
        val methods = mutableMapOf<String, FunctionType>()

        val linkage = if (isExtern) Linkage.External else Linkage.Internal

        producer {
            typeParameters.clear()
            fields.clear()
            methods.clear()

            StructType(
                identifier.text,
                typeParameters,
                fields,
                methods,
                classKeyword != null,
                linkage,
            ).also {
                it.psiElement = this@JaktStructDeclarationMixin
            }
        }

        initializer {
            if (genericBounds != null)
                typeParameters.addAll(getDeclGenericBounds().map { it.jaktType as TypeParameter })

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
        return structBody.structMemberList.mapNotNull { it.structField ?: it.structMethod?.function as JaktDeclaration }
    }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList.orEmpty()
}

val JaktStructDeclaration.isExtern: Boolean
    get() = externKeyword != null

val JaktStructDeclaration.isClass: Boolean
    get() = classKeyword != null
