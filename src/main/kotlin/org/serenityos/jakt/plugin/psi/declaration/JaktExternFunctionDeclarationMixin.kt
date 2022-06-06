package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktExternFunctionDeclaration
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktExternFunctionDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktExternFunctionDeclaration {
    // TODO: Deduplicate with JaktFunctionDeclarationMixin
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val name = identifier.text

            val typeParameters = if (genericBounds != null) {
                genericBounds!!.findChildrenOfType<JaktPlainQualifier>().map {
                    it.identifier.text
                }
            } else emptyList()

            val parameters = parameterList.map {
                Type.Function.Parameter(
                    it.identifier.text,
                    it.typeAnnotation.jaktType,
                    it.anonKeyword != null,
                    it.mutableKeyword != null,
                )
            }

            val returnType = functionReturnType.type?.jaktType ?: Type.Primitive.Void

            val type = Type.Function(
                name,
                typeParameters,
                null,
                parameters,
                returnType
            ).also {
                if (thisParameter != null) {
                    it.hasThis = true
                    it.thisIsMutable = thisParameter!!.mutableKeyword != null
                }
            }

            // TODO: Better caching
            CachedValueProvider.Result(type, this)
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}