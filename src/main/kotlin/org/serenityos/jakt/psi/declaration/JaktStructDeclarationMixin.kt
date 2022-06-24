package org.serenityos.jakt.psi.declaration

import com.intellij.lang.ASTNode
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.intellij.sdk.language.psi.JaktStructMethod
import org.serenityos.jakt.psi.named.JaktNamedElement
import org.serenityos.jakt.type.Type
import org.serenityos.jakt.type.unwrap
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktStructDeclarationMixin(
    node: ASTNode,
) : JaktNamedElement(node), JaktStructDeclaration {
    override val jaktType by recursivelyGuarded<Type> {
        val fields = mutableMapOf<String, Type>()
        val methods = mutableMapOf<String, Type.Function>()

        val linkage = if (isExtern) Type.Linkage.External else Type.Linkage.Internal

        producer {
            val typeParameters = if (genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            Type.Struct(
                identifier.text,
                fields,
                methods,
                linkage,
            ).let {
                it.declaration = this@JaktStructDeclarationMixin

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

        initializer { struct ->
            // TODO: Visibility
            val members = structBody.structMemberList.map { it.structMethod ?: it.structField }

            members.filterIsInstance<JaktStructField>().forEach {
                fields[it.identifier.text] = it.typeAnnotation.jaktType
            }

            members.filterIsInstance<JaktStructMethod>().forEach { method ->
                val type = method.functionDeclaration.jaktType.unwrap()
                require(type is Type.Function)

                if (type.hasThis && type.thisParameter == null) {
                    type.thisParameter = Type.Function.Parameter(
                        "this",
                        struct,
                        false,
                        type.thisIsMutable,
                    )
                }

                methods[type.name] = type
            }
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        return structBody.structMemberList.mapNotNull { it.structField ?: it.structMethod?.functionDeclaration }
    }

    override fun getDeclGenericBounds() = genericBounds?.genericBoundList ?: emptyList()
}

val JaktStructDeclaration.isExtern: Boolean
    get() = externKeyword != null

val JaktStructDeclaration.isClass: Boolean
    get() = classKeyword != null
