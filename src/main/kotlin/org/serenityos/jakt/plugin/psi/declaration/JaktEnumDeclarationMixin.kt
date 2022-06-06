package org.serenityos.jakt.plugin.psi.declaration

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.sdk.language.psi.JaktEnumDeclaration
import org.intellij.sdk.language.psi.impl.JaktTopLevelDefinitionImpl
import org.serenityos.jakt.plugin.psi.JaktPsiFactory
import org.serenityos.jakt.plugin.psi.api.JaktTypeable
import org.serenityos.jakt.plugin.type.Type

abstract class JaktEnumDeclarationMixin(
    node: ASTNode,
) : JaktTopLevelDefinitionImpl(node), JaktEnumDeclaration {
    override val jaktType: Type
        get() = CachedValuesManager.getCachedValue(this, JaktTypeable.TYPE_KEY) {
            // TODO: Variants
            val type = Type.Enum(
                name,
                normalEnumBody?.genericBounds?.plainQualifierList?.map { it.name!! } ?: emptyList(),
                underlyingTypeEnumBody?.typeAnnotation?.jaktType,
                emptyMap(),
            )

            CachedValueProvider.Result(type, this)
        }

    override fun getNameIdentifier() = identifier

    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String) = apply {
        nameIdentifier.replace(JaktPsiFactory(project).createIdentifier(name))
    }
}
