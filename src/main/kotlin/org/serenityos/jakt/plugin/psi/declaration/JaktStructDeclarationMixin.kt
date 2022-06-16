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

abstract class JaktStructDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktStructDeclaration, JaktNameIdentifierOwner, JaktDeclaration, JaktPsiScope {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val header = structHeader
            val body = structBody

            val typeParameters = if (header.genericBounds != null) {
                getDeclGenericBounds().map { Type.TypeVar(it.identifier.text) }
            } else emptyList()

            // TODO: Visibility
            val members = body.structMemberList.map { it.functionDeclaration ?: it.structField }
            val fields = members.filterIsInstance<JaktStructField>().associate {
                it.identifier.text to it.typeAnnotation.jaktType
            }
            val methods = members.filterIsInstance<JaktFunctionDeclaration>().associate {
                val type = it.jaktType
                require(type is Type.Function)
                it.identifier.text to type
            }

            val type = Type.Struct(
                header.identifier.text,
                fields,
                methods,
            ).let {
                if (typeParameters.isNotEmpty()) {
                    Type.Parameterized(it, typeParameters)
                } else it
            }

            // Populate our methods' thisParameters, if necessary
            methods.values.forEach {
                if (it.hasThis && it.thisParameter == null) {
                    it.thisParameter = Type.Function.Parameter(
                        "this",
                        type,
                        false,
                        it.thisIsMutable,
                    )
                }
            }

            // TODO: Better caching
            CachedValueProvider.Result(type, this)
        }

    override fun getDeclGenericBounds() = structHeader.genericBounds?.genericBoundList ?: emptyList()

    override fun getNameIdentifier(): PsiElement = structHeader.identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset
}
