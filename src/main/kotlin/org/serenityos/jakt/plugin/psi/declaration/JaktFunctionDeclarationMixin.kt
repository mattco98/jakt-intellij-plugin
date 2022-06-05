package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.*
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.*
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktFunctionDeclarationMixin(
    node: ASTNode,
) : ASTWrapperPsiElement(node), JaktFunctionDeclaration, JaktNameIdentifierOwner, JaktDeclaration {
    override val tracker = JaktModificationTracker()

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

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
