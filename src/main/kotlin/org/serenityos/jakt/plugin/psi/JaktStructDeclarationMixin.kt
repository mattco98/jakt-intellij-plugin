package org.serenityos.jakt.plugin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktFunctionDeclaration
import org.intellij.sdk.language.psi.JaktPlainQualifier
import org.intellij.sdk.language.psi.JaktStructDeclaration
import org.intellij.sdk.language.psi.JaktStructField
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.psi.declaration.JaktDeclaration
import org.serenityos.jakt.plugin.psi.declaration.JaktNameIdentifierOwner
import org.serenityos.jakt.plugin.type.Type
import org.serenityos.jakt.utils.findChildrenOfType

abstract class JaktStructDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktStructDeclaration, JaktNameIdentifierOwner, JaktDeclaration {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            val header = structHeader
            val body = structBody

            val typeParameters = if (header.genericBounds != null) {
                header.genericBounds!!.findChildrenOfType<JaktPlainQualifier>().map {
                    it.identifier.text
                }
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
                typeParameters,
                fields,
                methods,
            )

            // TODO: Better caching
            CachedValueProvider.Result(type, this)
        }

    override fun getNameIdentifier(): PsiElement = structHeader.identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
