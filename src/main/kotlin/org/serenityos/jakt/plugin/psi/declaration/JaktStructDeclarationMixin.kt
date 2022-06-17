package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktPsiScope
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.recursivelyGuarded

abstract class JaktStructDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktStructDeclaration, JaktNameIdentifierOwner, JaktDeclaration, JaktPsiScope {
    override val jaktType by recursivelyGuarded<Type> {
        val fields = mutableMapOf<String, Type>()
        val methods = mutableMapOf<String, Type.Function>()

        producer {
            val typeParameters = if (structHeader.genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            Type.Struct(
                structHeader.identifier.text,
                fields,
                methods,
            ).let {
                it.declaration = this@JaktStructDeclarationMixin

                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }
        }

        initializer { struct ->
            // TODO: Visibility
            val members = structBody.structMemberList.map { it.functionDeclaration ?: it.structField }

            members.filterIsInstance<JaktStructField>().forEach {
                fields[it.identifier.text] = it.typeAnnotation.jaktType
            }

            members.filterIsInstance<JaktFunctionDeclaration>().forEach {
                val type = it.jaktType
                require(type is Type.Function)

                if (type.hasThis && type.thisParameter == null) {
                    type.thisParameter = Type.Function.Parameter(
                        "this",
                        struct,
                        false,
                        type.thisIsMutable,
                    )
                }

                methods[it.identifier.text] = type
            }
        }
    }

    override fun getDeclarations(): List<JaktDeclaration> {
        return structBody.structMemberList.mapNotNull { it.structField ?: it.functionDeclaration }
    }

    override fun getDeclGenericBounds() = structHeader.genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier(): PsiElement = structHeader.identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset
}
